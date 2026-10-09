package com.example.fireview.domain.auth.service;

import com.example.fireview.domain.auth.service.RefreshTokenStore.Consumed;
import com.example.fireview.domain.auth.service.RefreshTokenStore.RefreshTokenRecord;
import com.example.fireview.domain.user.entity.User;
import com.example.fireview.domain.user.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * 리프레시 토큰 발급·회전·폐기 정책.
 *
 * <p>토큰은 32바이트 SecureRandom 값을 URL-safe Base64 로 만든 불투명 문자열이다.
 * 저장소에는 SHA-256 해시만 넘긴다. 원문은 응답(쿠키·앱 본문)으로만 나가고 로그에도 남기지 않는다.
 *
 * <p><b>회전</b>: refresh 할 때마다 옛 값을 원자적으로 소비하고 같은 패밀리로 새 값을 발급한다.
 * TTL 은 새 값 기준으로 다시 30분이 되므로 "30분 동안 refresh 가 없으면 만료" 가 된다.
 *
 * <p><b>동시 refresh(여러 탭)</b>: 탭 두 개가 같은 쿠키로 거의 동시에 refresh 하면 한쪽은
 * 이미 소비된 값을 내밀게 된다. 이걸 실패시키면 활동 중인 사용자가 로그아웃되므로, 소비된 지
 * {@code reuse-grace}(기본 15초) 안이면 <b>같은 패밀리로 새 값을 하나 더 발급</b>한다.
 * "같은 결과를 돌려주는" 방식은 새 원문을 저장해 둬야 해서 해시만 저장한다는 원칙과 충돌하므로
 * 택하지 않았다. 브라우저 쿠키 저장소에는 마지막에 도착한 응답의 값이 남고, 그 값도 유효하다.
 *
 * <p><b>재사용 탐지</b>: 유예를 넘겨 소비된 값이 다시 오면 탈취로 보고 그 패밀리 전체를 폐기한다.
 * 정상 사용자도 다음 refresh 에서 401 을 받아 다시 로그인하게 되지만, 탈취자의 세션도 함께 끊긴다.
 *
 * <p><b>저장소 장애</b>: 저장소 예외는 {@link RefreshTokenStoreUnavailableException}(503)으로 바꾼다.
 * 401 로 내보내면 살아 있는 세션까지 끊기기 때문이다. 처음 발급하는 경로(로그인·회원가입·OAuth)만
 * {@link #tryIssue} 로 리프레시 없이 진행할 수 있다. refresh·logout·전체 폐기는 성공으로 덮지 않는다
 * (fail-open 이면 폐기했다고 믿은 세션이 살아남는다).
 */
@Slf4j
@Service
public class RefreshTokenService {

    private static final int TOKEN_BYTES = 32;

    private final RefreshTokenStore store;
    private final UserRepository userRepository;
    private final Duration ttl;
    private final Duration reuseGrace;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    @Autowired
    public RefreshTokenService(RefreshTokenStore store,
                               UserRepository userRepository,
                               @Value("${app.auth.refresh.ttl:PT30M}") Duration ttl,
                               @Value("${app.auth.refresh.reuse-grace:PT15S}") Duration reuseGrace) {
        this(store, userRepository, ttl, reuseGrace, Clock.systemUTC());
    }

    RefreshTokenService(RefreshTokenStore store, UserRepository userRepository,
                        Duration ttl, Duration reuseGrace, Clock clock) {
        this.store = store;
        this.userRepository = userRepository;
        this.ttl = ttl;
        this.reuseGrace = reuseGrace;
        this.clock = clock;
    }

    /**
     * 로그인 성공 시 새 패밀리로 발급한다. 반환값은 원문이다.
     *
     * @throws RefreshTokenStoreUnavailableException 저장소에 쓰지 못했을 때
     */
    public String issue(User user) {
        RefreshTokenRecord record = new RefreshTokenRecord(user.getId(), UUID.randomUUID().toString());
        String token = newToken();
        withStore(() -> store.save(hash(token), record, clock.instant(), ttl, false));
        return token;
    }

    /**
     * 처음 로그인할 때 쓴다. 저장소 장애면 리프레시 토큰 없이 빈 값을 돌려준다.
     *
     * <p>리프레시 저장소가 생기기 전에는 로그인이 DB·JWT 만으로 끝났다. 저장소 장애 하나로
     * 정상 자격 증명 로그인·OAuth 까지 막히면 기존보다 가용성이 나빠지므로, 액세스 토큰만으로
     * 로그인은 성공시킨다. 그 세션은 액세스 토큰 만료와 함께 끝나고 다시 로그인하면 된다.
     * 이미 있는 세션을 다루는 refresh·logout 에는 이 완화를 쓰지 않는다.
     */
    public Optional<String> tryIssue(User user) {
        try {
            return Optional.of(issue(user));
        } catch (RefreshTokenStoreUnavailableException e) {
            log.warn("[RefreshToken] 저장소 장애로 리프레시 토큰 없이 로그인시킨다 - userId={}, cause={}",
                    user.getId(), e.getCause().getClass().getSimpleName());
            return Optional.empty();
        }
    }

    /**
     * 옛 값을 소비하고 같은 패밀리로 새 값을 발급한다.
     *
     * @throws InvalidRefreshTokenException 토큰이 없거나 만료·폐기·재사용됐거나 사용자가 없을 때
     */
    public Rotation rotate(String token) {
        if (token == null || token.isBlank()) {
            throw new InvalidRefreshTokenException();
        }
        Instant now = clock.instant();
        RefreshTokenRecord record = resolveForRotation(withStore(() -> store.consume(hash(token), now, ttl)), now);

        User user = userRepository.findById(record.userId()).orElseThrow(() -> {
            // 탈퇴 등으로 사용자가 사라졌다. 남은 세션도 정리한다
            withStore(() -> store.revokeAll(record.userId()));
            return new InvalidRefreshTokenException();
        });

        String next = newToken();
        if (!withStore(() -> store.save(hash(next), record, now, ttl, true))) {
            // 소비와 저장 사이에 전체 폐기(비밀번호 재설정 등)가 끼어들었다
            throw new InvalidRefreshTokenException();
        }
        return new Rotation(user, next);
    }

    /** 로그아웃. 토큰이 없거나 이미 무효여도 조용히 끝난다(멱등) */
    public void revoke(String token) {
        if (token == null || token.isBlank()) {
            return;
        }
        Consumed consumed = withStore(() -> store.consume(hash(token), clock.instant(), ttl));
        if (consumed instanceof Consumed.Live live) {
            withStore(() -> store.revokeFamily(live.record()));
        } else if (consumed instanceof Consumed.Used used) {
            withStore(() -> store.revokeFamily(used.record()));
        }
    }

    /**
     * 사용자의 모든 리프레시 토큰을 폐기한다. 비밀번호 재설정·회원 탈퇴 시.
     * 저장소 장애면 503 으로 실패시켜 호출한 트랜잭션(비밀번호 변경·탈퇴)도 되돌린다.
     */
    public void revokeAll(Long userId) {
        withStore(() -> store.revokeAll(userId));
    }

    public Duration getTtl() {
        return ttl;
    }

    private RefreshTokenRecord resolveForRotation(Consumed consumed, Instant now) {
        if (consumed instanceof Consumed.Live live) {
            return live.record();
        }
        if (consumed instanceof Consumed.Used used) {
            if (!used.consumedAt().plus(reuseGrace).isBefore(now)) {
                return used.record();
            }
            log.warn("[RefreshToken] 유예를 넘긴 재사용 감지. 패밀리를 폐기한다 - userId={}, familyId={}",
                    used.record().userId(), used.record().familyId());
            withStore(() -> store.revokeFamily(used.record()));
        }
        throw new InvalidRefreshTokenException();
    }

    /** 저장소 예외(연결 실패·타임아웃 등)를 503 으로 바꾼다. 메시지에 키가 섞일 수 있어 예외 종류만 남긴다 */
    private <T> T withStore(Supplier<T> action) {
        try {
            return action.get();
        } catch (DataAccessException e) {
            log.warn("[RefreshToken] 저장소 접근 실패 - cause={}", e.getClass().getSimpleName());
            throw new RefreshTokenStoreUnavailableException(e);
        }
    }

    private void withStore(Runnable action) {
        withStore(() -> {
            action.run();
            return null;
        });
    }

    private String newToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 을 쓸 수 없다", e);
        }
    }

    public record Rotation(User user, String refreshToken) {
    }
}

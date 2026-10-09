package com.example.fireview.domain.auth.service;

import com.example.fireview.domain.auth.service.RefreshTokenStore.Consumed;
import com.example.fireview.domain.auth.service.RefreshTokenStore.RefreshTokenRecord;
import com.example.fireview.domain.user.entity.User;
import com.example.fireview.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 리프레시 토큰 정책(해시 저장·TTL·회전·유예·재사용·전체 폐기)을 인메모리 저장소로 검증한다.
 * 시계를 직접 움직여 30분 TTL 과 15초 유예를 실제로 기다리지 않는다.
 */
class RefreshTokenServiceTest {

    private static final Duration TTL = Duration.ofMinutes(30);
    private static final Duration GRACE = Duration.ofSeconds(15);

    private final MutableClock clock = new MutableClock(Instant.parse("2026-10-09T00:00:00Z"));
    private InMemoryRefreshTokenStore store;
    private UserRepository userRepository;
    private RefreshTokenService service;
    private User user;

    @BeforeEach
    void setUp() {
        store = new InMemoryRefreshTokenStore();
        userRepository = mock(UserRepository.class);
        user = User.builder().id(7L).email("rt@fireview.com").nickname("세션").build();
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        service = new RefreshTokenService(store, userRepository, TTL, GRACE, clock);
    }

    // ── 발급·저장 ───────────────────────────────────────────────────────────

    @Test
    void 토큰은_32바이트_URL_safe_Base64다() {
        String token = service.issue(user);

        // 32바이트 → 패딩 없는 Base64 43자
        assertThat(token).hasSize(43).matches("[A-Za-z0-9_-]+");
        assertThat(service.issue(user)).isNotEqualTo(token);
    }

    @Test
    void 저장소에는_원문이_아니라_SHA256_해시만_들어간다() {
        String token = service.issue(user);

        assertThat(store.containsKey(token)).isFalse();
        assertThat(store.containsKey(RefreshTokenService.hash(token))).isTrue();
        assertThat(RefreshTokenService.hash(token)).hasSize(64).matches("[0-9a-f]+");
    }

    // ── TTL ────────────────────────────────────────────────────────────────

    @Test
    void 삼십분_동안_refresh_가_없으면_만료된다() {
        String token = service.issue(user);

        clock.advance(TTL);

        assertInvalid(token);
    }

    @Test
    void refresh_할_때마다_TTL_이_다시_삼십분이_된다() {
        String token = service.issue(user);

        // 29분마다 refresh 하면 처음 발급부터 한 시간이 지나도 살아 있다
        clock.advance(Duration.ofMinutes(29));
        String second = service.rotate(token).refreshToken();
        clock.advance(Duration.ofMinutes(29));
        String third = service.rotate(second).refreshToken();
        clock.advance(Duration.ofMinutes(29));

        assertThat(service.rotate(third).user()).isEqualTo(user);
    }

    // ── 회전 ───────────────────────────────────────────────────────────────

    @Test
    void 회전하면_새_값을_주고_사용자를_돌려준다() {
        String token = service.issue(user);

        RefreshTokenService.Rotation rotation = service.rotate(token);

        assertThat(rotation.refreshToken()).isNotEqualTo(token).hasSize(43);
        assertThat(rotation.user()).isEqualTo(user);
    }

    @Test
    void 모르는_토큰이나_빈_값은_실패한다() {
        assertInvalid("not-a-token");
        assertInvalid("");
        assertInvalid(null);
    }

    // ── 동시 refresh 유예 ──────────────────────────────────────────────────

    @Test
    void 유예_안에_옛_값이_다시_오면_새_값을_하나_더_발급한다() {
        String token = service.issue(user);
        String fromTabA = service.rotate(token).refreshToken();

        clock.advance(GRACE);
        String fromTabB = service.rotate(token).refreshToken();

        // 두 탭이 받은 값 모두 살아 있어야 어느 쪽 쿠키가 남아도 다음 refresh 가 된다
        assertThat(fromTabB).isNotEqualTo(fromTabA);
        assertThat(service.rotate(fromTabA).user()).isEqualTo(user);
        assertThat(service.rotate(fromTabB).user()).isEqualTo(user);
    }

    @Test
    void 동시에_같은_값으로_refresh_해도_모두_성공하고_소비는_한_번뿐이다() throws Exception {
        String token = service.issue(user);
        int tabs = 8;
        ExecutorService pool = Executors.newFixedThreadPool(tabs);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<String>> results = new ArrayList<>();
        Callable<String> refresh = () -> {
            start.await();
            return service.rotate(token).refreshToken();
        };
        for (int i = 0; i < tabs; i++) {
            results.add(pool.submit(refresh));
        }
        start.countDown();

        List<String> issued = new ArrayList<>();
        for (Future<String> result : results) {
            issued.add(result.get(5, TimeUnit.SECONDS));
        }
        pool.shutdown();

        assertThat(issued).doesNotHaveDuplicates().hasSize(tabs);
        // 옛 값은 이미 소비됐다. 저장소에 다시 내밀면 Used 로 나온다
        assertThat(store.consume(RefreshTokenService.hash(token), clock.instant(), TTL))
                .isInstanceOf(Consumed.Used.class);
    }

    // ── 재사용 탐지 ────────────────────────────────────────────────────────

    @Test
    void 유예를_넘겨_옛_값이_다시_오면_실패하고_그_패밀리_전체를_폐기한다() {
        String token = service.issue(user);
        String next = service.rotate(token).refreshToken();

        clock.advance(GRACE.plusMillis(1));
        assertInvalid(token);

        // 정상 사용자가 갖고 있던 최신 값도 함께 죽는다 (탈취자 세션을 끊기 위한 대가)
        assertInvalid(next);
    }

    @Test
    void 재사용으로_폐기돼도_다른_기기의_로그인은_유지된다() {
        String laptop = service.issue(user);
        String phone = service.issue(user);
        service.rotate(laptop);

        clock.advance(Duration.ofMinutes(1));
        assertInvalid(laptop);

        assertThat(service.rotate(phone).user()).isEqualTo(user);
    }

    // ── 폐기 ───────────────────────────────────────────────────────────────

    @Test
    void 전체_폐기하면_모든_기기의_토큰이_실패한다() {
        String laptop = service.issue(user);
        String phone = service.rotate(service.issue(user)).refreshToken();

        service.revokeAll(user.getId());

        assertInvalid(laptop);
        assertInvalid(phone);
    }

    @Test
    void 로그아웃하면_그_패밀리가_폐기되고_반복해도_오류가_없다() {
        String token = service.issue(user);
        String next = service.rotate(token).refreshToken();

        service.revoke(next);
        service.revoke(next);
        service.revoke("unknown");
        service.revoke(null);

        assertInvalid(next);
        // 유예 안의 옛 값으로 되살릴 수도 없다
        assertInvalid(token);
    }

    @Test
    void 사용자가_사라졌으면_실패하고_남은_토큰도_정리한다() {
        String token = service.issue(user);
        String other = service.issue(user);
        when(userRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertInvalid(token);

        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        assertInvalid(other);
    }

    @Test
    void 소비와_저장_사이에_전체_폐기가_끼어들면_새_값을_저장하지_않는다() {
        RefreshTokenRecord record = new RefreshTokenRecord(7L, "family-1");
        store.save("h1", record, clock.instant(), TTL, false);
        store.consume("h1", clock.instant(), TTL);

        store.revokeAll(7L);

        assertThat(store.save("h2", record, clock.instant(), TTL, true)).isFalse();
        assertThat(store.containsKey("h2")).isFalse();
    }

    private void assertInvalid(String token) {
        assertThatThrownBy(() -> service.rotate(token))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    /** 테스트에서 시간을 앞으로 돌리기 위한 시계 */
    static final class MutableClock extends Clock {
        private volatile Instant now;

        MutableClock(Instant now) {
            this.now = now;
        }

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}

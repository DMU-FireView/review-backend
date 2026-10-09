package com.example.fireview.domain.auth.service;

import java.time.Duration;
import java.time.Instant;

/**
 * 리프레시 토큰 저장소.
 *
 * <p>키는 언제나 토큰 원문의 SHA-256 해시다. 원문은 이 계층에 들어오지 않는다.
 * 저장소가 유출돼도 해시만으로는 쿠키를 만들 수 없다.
 *
 * <p>회전·유예·재사용 판단은 {@link RefreshTokenService} 가 하고, 여기서는
 * 원자적으로 해야 하는 상태 변경만 맡는다.
 *
 * <ul>
 *   <li>기본 구현: {@link InMemoryRefreshTokenStore} (로컬·테스트)</li>
 *   <li>운영: {@link RedisRefreshTokenStore} ({@code app.auth.redis-token-store.enabled=true})</li>
 * </ul>
 */
public interface RefreshTokenStore {

    /**
     * 살아 있는 토큰을 저장하고 패밀리·사용자 인덱스에 묶는다.
     *
     * @param requireFamily true 면 패밀리가 이미 있을 때만 저장한다(회전용). 그 사이에
     *                      전체 폐기가 끼어들었다면 저장하지 않고 false 를 돌려준다.
     * @return 저장했으면 true
     */
    boolean save(String tokenHash, RefreshTokenRecord record, Instant now, Duration ttl, boolean requireFamily);

    /**
     * 토큰을 원자적으로 소비한다.
     *
     * <p>살아 있으면 지우고 "사용됨" 표시(소비 시각 포함)를 남긴 뒤 {@link Consumed.Live} 를,
     * 이미 사용된 토큰이면 {@link Consumed.Used} 를, 모르는 토큰이면 {@link Consumed.Missing} 을 돌려준다.
     * 동시에 두 요청이 와도 {@code Live} 는 정확히 한 번만 나온다.
     */
    Consumed consume(String tokenHash, Instant now, Duration ttl);

    /** 패밀리(한 번의 로그인에서 회전으로 이어진 토큰들) 전체를 폐기한다 */
    void revokeFamily(RefreshTokenRecord record);

    /** 사용자의 모든 패밀리를 폐기한다. 비밀번호 재설정·탈퇴 시 */
    void revokeAll(Long userId);

    record RefreshTokenRecord(Long userId, String familyId) {
    }

    sealed interface Consumed {
        record Live(RefreshTokenRecord record) implements Consumed {
        }

        record Used(RefreshTokenRecord record, Instant consumedAt) implements Consumed {
        }

        record Missing() implements Consumed {
        }
    }
}

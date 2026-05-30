package com.example.fireview.domain.auth.service;

import com.example.fireview.global.exception.CustomException;
import com.example.fireview.global.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

// TODO: 운영 단계에서는 Redis(Spring Data Redis) 기반 저장소로 교체.
//       현재는 단일 인스턴스 가정의 인메모리 구현이며, 스케일 아웃 시 토큰이 공유되지 않음.
@Slf4j
@Component
public class PasswordResetTokenStore {

    private static final Duration TTL = Duration.ofMinutes(15);

    private record Entry(String email, Instant expiresAt) {}

    private final Map<String, Entry> store = new ConcurrentHashMap<>();

    public String issue(String email) {
        String token = UUID.randomUUID().toString();
        store.put(token, new Entry(email, Instant.now().plus(TTL)));
        return token;
    }

    /**
     * 토큰을 소비하고 이메일을 반환한다.
     *
     * 기존 문제: store.remove() 로 먼저 삭제 후 만료 여부를 체크해서
     *           만료 토큰과 존재하지 않는 토큰을 구분할 수 없었음.
     *
     * 수정: 존재 여부 → 만료 여부 순으로 체크한 뒤 삭제.
     *       만료 시 로그를 남겨 디버깅/모니터링 가능하도록 개선.
     *
     * @throws CustomException INVALID_RESET_TOKEN - 토큰 없음 또는 만료
     */
    public String consume(String token) {
        Entry entry = store.get(token);

        if (entry == null) {
            log.warn("[PasswordReset] 존재하지 않는 토큰 사용 시도");
            throw new CustomException(ErrorCode.INVALID_RESET_TOKEN);
        }

        if (entry.expiresAt().isBefore(Instant.now())) {
            store.remove(token); // 만료된 토큰 정리
            log.warn("[PasswordReset] 만료된 토큰 사용 시도: email={}", entry.email());
            throw new CustomException(ErrorCode.EXPIRED_RESET_TOKEN);
        }

        store.remove(token); // 유효한 토큰 소비 (1회용)
        return entry.email();
    }
}

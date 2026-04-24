package com.example.fireview.domain.auth.service;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

// TODO: 운영 단계에서는 Redis(Spring Data Redis) 기반 저장소로 교체.
//       현재는 단일 인스턴스 가정의 인메모리 구현이며, 스케일 아웃 시 토큰이 공유되지 않음.
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

    public String consume(String token) {
        Entry entry = store.remove(token);
        if (entry == null || entry.expiresAt().isBefore(Instant.now())) {
            return null;
        }
        return entry.email();
    }
}

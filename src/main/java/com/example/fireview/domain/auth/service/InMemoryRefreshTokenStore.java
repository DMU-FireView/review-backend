package com.example.fireview.domain.auth.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 인메모리 리프레시 토큰 저장소 (기본 구현, 로컬·테스트용).
 *
 * <p>단일 인스턴스에서만 의미가 있고 재시작하면 모든 세션이 끊긴다.
 * 운영은 {@code app.auth.redis-token-store.enabled=true} 로 {@link RedisRefreshTokenStore} 를 쓴다.
 *
 * <p>모든 메서드를 {@code synchronized} 로 묶어 Redis Lua 스크립트와 같은 원자성을 낸다.
 * 만료는 호출 시각({@code now})으로 판단하므로 테스트에서 시간을 직접 움직일 수 있다.
 */
@Component
@ConditionalOnProperty(name = "app.auth.redis-token-store.enabled", havingValue = "false", matchIfMissing = true)
public class InMemoryRefreshTokenStore implements RefreshTokenStore {

    private record Entry(RefreshTokenRecord record, Instant expiresAt, Instant consumedAt) {
        boolean isExpired(Instant now) {
            return !expiresAt.isAfter(now);
        }
    }

    private final Map<String, Entry> live = new HashMap<>();
    private final Map<String, Entry> used = new HashMap<>();
    private final Map<String, Set<String>> families = new HashMap<>();
    private final Map<Long, Set<String>> userFamilies = new HashMap<>();

    @Override
    public synchronized boolean save(String tokenHash, RefreshTokenRecord record, Instant now,
                                     Duration ttl, boolean requireFamily) {
        if (requireFamily && !families.containsKey(record.familyId())) {
            return false;
        }
        live.put(tokenHash, new Entry(record, now.plus(ttl), null));
        families.computeIfAbsent(record.familyId(), k -> new HashSet<>()).add(tokenHash);
        userFamilies.computeIfAbsent(record.userId(), k -> new HashSet<>()).add(record.familyId());
        return true;
    }

    @Override
    public synchronized Consumed consume(String tokenHash, Instant now, Duration ttl) {
        Entry entry = live.remove(tokenHash);
        if (entry != null && !entry.isExpired(now)) {
            used.put(tokenHash, new Entry(entry.record(), now.plus(ttl), now));
            return new Consumed.Live(entry.record());
        }
        Entry usedEntry = used.get(tokenHash);
        if (usedEntry != null && !usedEntry.isExpired(now)) {
            return new Consumed.Used(usedEntry.record(), usedEntry.consumedAt());
        }
        used.remove(tokenHash);
        return new Consumed.Missing();
    }

    @Override
    public synchronized void revokeFamily(RefreshTokenRecord record) {
        removeFamily(record.familyId());
        Set<String> ids = userFamilies.get(record.userId());
        if (ids != null) {
            ids.remove(record.familyId());
        }
    }

    @Override
    public synchronized void revokeAll(Long userId) {
        Set<String> ids = userFamilies.remove(userId);
        if (ids != null) {
            ids.forEach(this::removeFamily);
        }
    }

    /** 테스트에서 "원문이 키로 저장되지 않는다"를 확인하기 위한 통로 */
    synchronized boolean containsKey(String key) {
        return live.containsKey(key) || used.containsKey(key);
    }

    private void removeFamily(String familyId) {
        Set<String> hashes = families.remove(familyId);
        if (hashes != null) {
            hashes.forEach(hash -> {
                live.remove(hash);
                used.remove(hash);
            });
        }
    }
}

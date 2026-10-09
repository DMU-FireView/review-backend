package com.example.fireview.domain.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Redis 기반 리프레시 토큰 저장소.
 * {@code app.auth.redis-token-store.enabled=true} 일 때 {@link InMemoryRefreshTokenStore} 를 대체한다
 * (비밀번호 재설정 토큰 저장소와 같은 스위치).
 *
 * <pre>
 * rt:{hash}          → "userId:familyId"              살아 있는 토큰, TTL = 리프레시 TTL
 * rt:used:{hash}     → "userId:familyId:consumedMs"   회전으로 소비된 토큰(유예·재사용 판단용), TTL 동일
 * rt:fam:{familyId}  → Set(hash)                      패밀리 폐기용 인덱스, 저장할 때마다 TTL 갱신
 * rt:user:{userId}   → Set(familyId)                  사용자 전체 폐기용 인덱스, 저장할 때마다 TTL 갱신
 * </pre>
 *
 * <p>인덱스 Set 의 TTL 은 가장 최근에 저장한 토큰의 TTL 과 같다. 그 안의 토큰은 모두 그보다
 * 먼저 만료되므로, 인덱스가 사라질 때 살아 있는 토큰이 남지 않는다.
 *
 * <p>인덱스 키는 저장할 때마다 TTL 이 늘어나서, 계속 활동하는 사용자는 키가 만료되지 않는다.
 * 그러면 회전할 때마다 패밀리 Set 에 지난 해시가, 사용자 Set 에 끝난 패밀리 ID 가 무한히 쌓이고
 * 폐기 스크립트의 SMEMBERS/DEL 도 그만큼 느려진다. 그래서 저장할 때 가리키는 키가 이미 사라진
 * 멤버(live·used 둘 다 없는 해시, 패밀리 키가 없는 패밀리 ID)를 같이 지운다. 남는 멤버는
 * 최근 TTL 안에 저장·소비된 것뿐이라 크기가 TTL 동안의 회전·로그인 수로 묶인다.
 *
 * <p>모든 상태 변경은 Lua 스크립트 하나로 실행돼 원자적이다. 스크립트 안에서 키 이름을
 * 만들기 때문에 Redis Cluster 에서는 쓸 수 없다(운영은 단일 노드).
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.auth.redis-token-store.enabled", havingValue = "true")
public class RedisRefreshTokenStore implements RefreshTokenStore {

    static final String LIVE_PREFIX = "rt:";
    static final String USED_PREFIX = "rt:used:";
    static final String FAMILY_PREFIX = "rt:fam:";
    static final String USER_PREFIX = "rt:user:";

    /** KEYS: live, family, user / ARGV: value, ttlMs, hash, familyId, requireFamily, livePrefix, usedPrefix, familyPrefix */
    private static final RedisScript<Long> SAVE = new DefaultRedisScript<>("""
            if ARGV[5] == '1' and redis.call('EXISTS', KEYS[2]) == 0 then
              return 0
            end
            for _, h in ipairs(redis.call('SMEMBERS', KEYS[2])) do
              if redis.call('EXISTS', ARGV[6] .. h, ARGV[7] .. h) == 0 then
                redis.call('SREM', KEYS[2], h)
              end
            end
            for _, f in ipairs(redis.call('SMEMBERS', KEYS[3])) do
              if f ~= ARGV[4] and redis.call('EXISTS', ARGV[8] .. f) == 0 then
                redis.call('SREM', KEYS[3], f)
              end
            end
            redis.call('SET', KEYS[1], ARGV[1], 'PX', ARGV[2])
            redis.call('SADD', KEYS[2], ARGV[3])
            redis.call('PEXPIRE', KEYS[2], ARGV[2])
            redis.call('SADD', KEYS[3], ARGV[4])
            redis.call('PEXPIRE', KEYS[3], ARGV[2])
            return 1
            """, Long.class);

    /** KEYS: live, used / ARGV: nowMs, ttlMs. 반환 "L:값" | "U:값" | nil */
    private static final RedisScript<String> CONSUME = new DefaultRedisScript<>("""
            local v = redis.call('GET', KEYS[1])
            if v then
              redis.call('DEL', KEYS[1])
              redis.call('SET', KEYS[2], v .. ':' .. ARGV[1], 'PX', ARGV[2])
              return 'L:' .. v
            end
            local u = redis.call('GET', KEYS[2])
            if u then
              return 'U:' .. u
            end
            return false
            """, String.class);

    /** KEYS: family, user / ARGV: familyId, livePrefix, usedPrefix */
    private static final RedisScript<Long> REVOKE_FAMILY = new DefaultRedisScript<>("""
            for _, h in ipairs(redis.call('SMEMBERS', KEYS[1])) do
              redis.call('DEL', ARGV[2] .. h, ARGV[3] .. h)
            end
            redis.call('DEL', KEYS[1])
            redis.call('SREM', KEYS[2], ARGV[1])
            return 1
            """, Long.class);

    /** KEYS: user / ARGV: familyPrefix, livePrefix, usedPrefix */
    private static final RedisScript<Long> REVOKE_ALL = new DefaultRedisScript<>("""
            for _, f in ipairs(redis.call('SMEMBERS', KEYS[1])) do
              local fk = ARGV[1] .. f
              for _, h in ipairs(redis.call('SMEMBERS', fk)) do
                redis.call('DEL', ARGV[2] .. h, ARGV[3] .. h)
              end
              redis.call('DEL', fk)
            end
            redis.call('DEL', KEYS[1])
            return 1
            """, Long.class);

    private final StringRedisTemplate redisTemplate;

    @Override
    public boolean save(String tokenHash, RefreshTokenRecord record, Instant now, Duration ttl, boolean requireFamily) {
        Long saved = redisTemplate.execute(SAVE,
                List.of(LIVE_PREFIX + tokenHash, FAMILY_PREFIX + record.familyId(), USER_PREFIX + record.userId()),
                record.userId() + ":" + record.familyId(),
                String.valueOf(ttl.toMillis()),
                tokenHash,
                record.familyId(),
                requireFamily ? "1" : "0",
                LIVE_PREFIX, USED_PREFIX, FAMILY_PREFIX);
        return saved != null && saved == 1L;
    }

    @Override
    public Consumed consume(String tokenHash, Instant now, Duration ttl) {
        String result = redisTemplate.execute(CONSUME,
                List.of(LIVE_PREFIX + tokenHash, USED_PREFIX + tokenHash),
                String.valueOf(now.toEpochMilli()),
                String.valueOf(ttl.toMillis()));
        if (result == null) {
            return new Consumed.Missing();
        }
        String[] parts = result.substring(2).split(":");
        try {
            RefreshTokenRecord record = new RefreshTokenRecord(Long.parseLong(parts[0]), parts[1]);
            if (result.startsWith("L:")) {
                return new Consumed.Live(record);
            }
            return new Consumed.Used(record, Instant.ofEpochMilli(Long.parseLong(parts[2])));
        } catch (RuntimeException e) {
            // 값 형식이 깨졌으면 모르는 토큰으로 본다. 해시도 로그에 남기지 않는다
            log.warn("[RefreshToken:Redis] 저장된 값 형식이 올바르지 않다");
            return new Consumed.Missing();
        }
    }

    @Override
    public void revokeFamily(RefreshTokenRecord record) {
        redisTemplate.execute(REVOKE_FAMILY,
                List.of(FAMILY_PREFIX + record.familyId(), USER_PREFIX + record.userId()),
                record.familyId(), LIVE_PREFIX, USED_PREFIX);
    }

    @Override
    public void revokeAll(Long userId) {
        redisTemplate.execute(REVOKE_ALL,
                List.of(USER_PREFIX + userId),
                FAMILY_PREFIX, LIVE_PREFIX, USED_PREFIX);
    }
}

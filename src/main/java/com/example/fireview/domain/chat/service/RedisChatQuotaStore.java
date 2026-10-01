package com.example.fireview.domain.chat.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Redis 기반 하루 사용량 카운터.
 * {@code app.chat.redis-quota.enabled=true} 일 때 {@link ChatQuotaStore} 빈을 대체한다.
 *
 * <p>INCR 가 원자적이라 인스턴스가 여러 대여도 한도가 하나로 공유되고,
 * 서버를 재시작해도 사용량이 유지된다. 날짜별 키의 만료는 Redis 가 처리한다.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.chat.redis-quota.enabled", havingValue = "true")
public class RedisChatQuotaStore extends ChatQuotaStore {

    private static final String KEY_PREFIX = "chat:quota:";

    private final StringRedisTemplate redisTemplate;

    public RedisChatQuotaStore(StringRedisTemplate redisTemplate,
                               @Value("${app.chat.quota.zone:Asia/Seoul}") String zoneId) {
        super(zoneId);
        this.redisTemplate = redisTemplate;
    }

    @Override
    public boolean tryConsume(long userId, int limit) {
        if (limit == 0) return false;

        String key = redisKey(userId);
        Long after = redisTemplate.opsForValue().increment(key);
        if (after == null) {
            // Redis 가 응답하지 않는 상황. 과금 경계보다 서비스 연속성을 택해 통과시킨다.
            log.warn("[ChatQuota] INCR 응답이 비어 있다. 한도 검사를 건너뛴다 - userId={}", userId);
            return true;
        }

        // 매번 TTL 을 다시 건다. 증가 직후에만 걸면 그 사이에 프로세스가 죽었을 때
        // 만료 없는 키가 남아 다음 날까지 사용량이 이어진다. 같은 자정을 가리키므로
        // 몇 번을 걸어도 결과는 같다.
        redisTemplate.expire(key, untilMidnight());

        if (limit != UNLIMITED && after > limit) {
            redisTemplate.opsForValue().decrement(key);
            return false;
        }
        return true;
    }

    @Override
    public int used(long userId) {
        String value = redisTemplate.opsForValue().get(redisKey(userId));
        if (value == null) return 0;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            log.warn("[ChatQuota] 사용량 값이 숫자가 아니다 - userId={}, value={}", userId, value);
            return 0;
        }
    }

    @Override
    public void refund(long userId) {
        String key = redisKey(userId);
        Long after = redisTemplate.opsForValue().decrement(key);
        if (after != null && after < 0) {
            // 환불이 중복으로 들어온 경우. 음수로 남겨두면 한도가 늘어난 것처럼 보인다.
            redisTemplate.opsForValue().set(key, "0", untilMidnight());
        }
    }

    private String redisKey(long userId) {
        return KEY_PREFIX + key(userId);
    }
}

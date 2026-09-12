package com.example.fireview.domain.webhook.service;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Redis 기반 처리 완료 웹훅 저장소.
 * app.webhook.redis-dedup.enabled=true 일 때 ProcessedWebhookStore 빈을 대체한다.
 *
 * SET NX 로 원자적으로 표시하므로 동시에 같은 웹훅이 두 번 들어와도 하나만 통과한다.
 * TTL 만료는 Redis가 처리한다.
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.webhook.redis-dedup.enabled", havingValue = "true")
public class RedisProcessedWebhookStore extends ProcessedWebhookStore {

    private static final String KEY_PREFIX = "webhook:processed:";

    private final StringRedisTemplate redisTemplate;

    @Override
    public boolean markIfAbsent(String jobId) {
        Boolean created = redisTemplate.opsForValue().setIfAbsent(KEY_PREFIX + jobId, "1", TTL);
        return Boolean.TRUE.equals(created);
    }

    @Override
    public void release(String jobId) {
        redisTemplate.delete(KEY_PREFIX + jobId);
    }
}

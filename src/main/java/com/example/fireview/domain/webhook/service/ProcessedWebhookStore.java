package com.example.fireview.domain.webhook.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 처리 완료된 웹훅 jobId 를 기억하는 인메모리 저장소 (기본 구현).
 * 운영 환경에서는 app.webhook.redis-dedup.enabled=true 설정 시
 * RedisProcessedWebhookStore 가 이 빈을 대체한다.
 *
 * 웹훅은 재전송이 정상 동작이므로, 같은 jobId 가 두 번 와도 알림이 두 번 나가지 않게 막는다.
 * 단일 인스턴스에서만 유효하며 서버 재시작 시 초기화된다.
 */
@Component
@ConditionalOnProperty(name = "app.webhook.redis-dedup.enabled", havingValue = "false", matchIfMissing = true)
public class ProcessedWebhookStore {

    protected static final Duration TTL = Duration.ofHours(24);

    private final Map<String, Instant> processed = new ConcurrentHashMap<>();

    /**
     * jobId 를 처리 중으로 표시한다.
     *
     * @return 처음 보는 jobId 면 true, 이미 처리했거나 처리 중이면 false
     */
    public boolean markIfAbsent(String jobId) {
        evictExpired();
        return processed.putIfAbsent(jobId, Instant.now().plus(TTL)) == null;
    }

    /** 처리 도중 실패했을 때 표시를 되돌려 재전송이 다시 처리되도록 한다. */
    public void release(String jobId) {
        processed.remove(jobId);
    }

    private void evictExpired() {
        Instant now = Instant.now();
        processed.entrySet().removeIf(e -> e.getValue().isBefore(now));
    }
}

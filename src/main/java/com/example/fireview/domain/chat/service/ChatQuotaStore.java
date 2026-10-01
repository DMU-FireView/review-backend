package com.example.fireview.domain.chat.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 하루 챗봇 메시지 사용량 카운터 (인메모리 기본 구현).
 *
 * <p>운영 환경에서는 {@code app.chat.redis-quota.enabled=true} 로
 * {@link RedisChatQuotaStore} 가 이 빈을 대체한다. 인메모리 구현은 인스턴스마다
 * 카운터가 따로 쌓이므로 다중 인스턴스에서는 한도가 사실상 인스턴스 수만큼 늘어난다.
 * 또 재시작하면 0 으로 돌아간다. 과금 경계를 지켜야 하는 환경에서는 Redis 를 쓴다.
 *
 * <p><b>하루 기준</b>은 {@code app.chat.quota.zone}(기본 Asia/Seoul) 의 자정이다.
 * UTC 로 끊으면 한국 사용자에게는 오전 9시에 한도가 초기화돼 설명이 안 된다.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.chat.redis-quota.enabled", havingValue = "false", matchIfMissing = true)
public class ChatQuotaStore {

    /** 한도로 쓰면 무제한을 뜻하는 값. 사용량은 계속 세지만 거절하지 않는다 */
    public static final int UNLIMITED = -1;

    protected final ZoneId zone;

    /** key = "{userId}:{yyyy-MM-dd}" */
    private final Map<String, AtomicInteger> counters = new ConcurrentHashMap<>();

    public ChatQuotaStore(@Value("${app.chat.quota.zone:Asia/Seoul}") String zoneId) {
        this.zone = ZoneId.of(zoneId);
    }

    /**
     * 사용량을 1 늘린다. 한도를 넘으면 늘리지 않는다.
     *
     * <p>먼저 올리고 넘으면 되돌리는 순서인 이유: 읽고 나서 쓰면 동시 요청 둘이
     * 같은 값을 읽어 한도를 한 칸 넘길 수 있다. 증가가 원자적이면 그 틈이 없다.
     *
     * @return 사용 가능해서 차감했으면 true, 한도 초과면 false
     */
    public boolean tryConsume(long userId, int limit) {
        if (limit == 0) return false;

        AtomicInteger counter = counters.computeIfAbsent(key(userId), k -> {
            evictOtherDays(userId);
            return new AtomicInteger();
        });
        int after = counter.incrementAndGet();

        if (limit != UNLIMITED && after > limit) {
            counter.decrementAndGet();
            return false;
        }
        return true;
    }

    /** 오늘 사용량 */
    public int used(long userId) {
        AtomicInteger counter = counters.get(key(userId));
        return counter == null ? 0 : counter.get();
    }

    /**
     * 차감을 되돌린다.
     *
     * <p>LLM 호출이 실패해 답변을 못 준 턴은 사용량으로 세지 않는다.
     * 서버 잘못으로 사용자 한도를 깎으면 안 된다.
     */
    public void refund(long userId) {
        AtomicInteger counter = counters.get(key(userId));
        if (counter != null) {
            counter.updateAndGet(v -> v > 0 ? v - 1 : 0);
        }
    }

    /** 한도가 초기화되는 시각 (다음 자정) */
    public Instant resetAt() {
        return nextMidnight().toInstant();
    }

    // ────────────────────────────── 내부 ──────────────────────────────

    protected String key(long userId) {
        return userId + ":" + LocalDate.now(zone);
    }

    /** 남은 TTL. Redis 구현이 쓰고, 인메모리는 날짜가 바뀐 키를 직접 지운다 */
    protected Duration untilMidnight() {
        return Duration.between(ZonedDateTime.now(zone), nextMidnight());
    }

    private ZonedDateTime nextMidnight() {
        return LocalDate.now(zone).plusDays(1).atStartOfDay(zone);
    }

    /** 날짜가 바뀌면 그 사용자의 어제 키는 쓸모가 없다. 새 키를 만들 때 같이 치운다 */
    private void evictOtherDays(long userId) {
        String todaySuffix = ":" + LocalDate.now(zone);
        String prefix = userId + ":";
        counters.keySet().removeIf(k -> k.startsWith(prefix) && !k.endsWith(todaySuffix));
    }
}

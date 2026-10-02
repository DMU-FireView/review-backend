package com.example.fireview.domain.chat.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class ChatQuotaStoreTest {

    private static final long USER = 1L;

    private ChatQuotaStore store;

    @BeforeEach
    void setUp() {
        store = new ChatQuotaStore("Asia/Seoul");
    }

    @Test
    void 한도까지만_통과시킨다() {
        for (int i = 0; i < 5; i++) {
            assertThat(store.tryConsume(USER, 5)).isTrue();
        }
        assertThat(store.tryConsume(USER, 5)).isFalse();
        assertThat(store.used(USER)).isEqualTo(5);
    }

    @Test
    void 거절된_요청은_사용량을_늘리지_않는다() {
        store.tryConsume(USER, 1);
        store.tryConsume(USER, 1);
        store.tryConsume(USER, 1);

        // 거절될 때마다 올라가면 환불 후에도 한도가 복구되지 않는다
        assertThat(store.used(USER)).isEqualTo(1);
    }

    @Test
    void 사용자별로_따로_센다() {
        store.tryConsume(USER, 5);
        store.tryConsume(USER, 5);

        assertThat(store.used(2L)).isZero();
        assertThat(store.tryConsume(2L, 5)).isTrue();
        assertThat(store.used(USER)).isEqualTo(2);
    }

    @Test
    void 환불하면_다시_쓸_수_있다() {
        for (int i = 0; i < 5; i++) store.tryConsume(USER, 5);
        assertThat(store.tryConsume(USER, 5)).isFalse();

        store.refund(USER);

        assertThat(store.used(USER)).isEqualTo(4);
        assertThat(store.tryConsume(USER, 5)).isTrue();
    }

    @Test
    void 쓴_적_없는데_환불해도_음수가_되지_않는다() {
        store.refund(USER);
        store.refund(USER);

        // 음수로 남으면 한도가 늘어난 것처럼 동작한다
        assertThat(store.used(USER)).isZero();
    }

    @Test
    void 한도가_0이면_아무도_통과하지_못한다() {
        assertThat(store.tryConsume(USER, 0)).isFalse();
        assertThat(store.used(USER)).isZero();
    }

    @Test
    void 무제한이면_거절하지_않고_사용량만_센다() {
        for (int i = 0; i < 50; i++) {
            assertThat(store.tryConsume(USER, ChatQuotaStore.UNLIMITED)).isTrue();
        }
        assertThat(store.used(USER)).isEqualTo(50);
    }

    @Test
    void 초기화_시각은_미래다() {
        assertThat(store.resetAt()).isAfter(Instant.now());
    }

    @Test
    void 동시_요청이_한도를_넘기지_못한다() throws Exception {
        int limit = 5;
        int threads = 50;
        ExecutorService pool = Executors.newFixedThreadPool(16);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        AtomicInteger passed = new AtomicInteger();

        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                try {
                    start.await();
                    if (store.tryConsume(USER, limit)) passed.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }
        start.countDown();
        assertThat(done.await(10, TimeUnit.SECONDS)).isTrue();
        pool.shutdownNow();

        // 읽고 나서 쓰는 방식이면 여기서 한도를 넘긴다
        assertThat(passed.get()).isEqualTo(limit);
        assertThat(store.used(USER)).isEqualTo(limit);
    }
}

package com.example.fireview.domain.ai.service;

import com.example.fireview.domain.ai.client.AiServerClient;
import com.example.fireview.domain.notification.service.NotificationService;
import com.example.fireview.domain.product.repository.ProductRepository;
import com.example.fireview.domain.review.repository.ReviewRepository;
import com.example.fireview.domain.user.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiAnalysisServiceTest {

    @Mock AiServerClient aiServerClient;
    @Mock ProductRepository productRepository;
    @Mock ReviewRepository reviewRepository;
    @Mock NotificationService notificationService;
    @Mock UserService userService;

    private final ExecutorService executor = Executors.newFixedThreadPool(3, r -> {
        Thread t = new Thread(r);
        t.setName("ai-call-test-" + t.getId());
        return t;
    });

    @AfterEach
    void shutdown() {
        executor.shutdownNow();
    }

    /**
     * 3개 호출이 정말 동시에 실행되는지 검증한다.
     *
     * 각 호출은 "다른 두 호출도 시작될 때까지" 대기한다(latch).
     * 순차 실행이면 첫 호출이 영원히 기다리므로 타임아웃으로 실패하고,
     * 병렬 실행이면 세 호출이 서로를 풀어줘 정상 완료된다.
     */
    @Test
    void AI_서버_3개_호출은_주입된_executor에서_동시에_실행된다() {
        CountDownLatch allStarted = new CountDownLatch(3);
        Set<String> threadNames = ConcurrentHashMap.newKeySet();

        when(aiServerClient.analyzeProductList(any())).thenAnswer(inv -> waitForOthers(allStarted, threadNames));
        when(aiServerClient.analyzeProductDetail(any())).thenAnswer(inv -> waitForOthers(allStarted, threadNames));
        when(aiServerClient.analyzeRtiTrend(any())).thenAnswer(inv -> waitForOthers(allStarted, threadNames));
        when(aiServerClient.analyzeProductRiskReport(any())).thenReturn(null);

        AiAnalysisService service = new AiAnalysisService(
                aiServerClient, productRepository, reviewRepository, notificationService, userService, executor, new com.example.fireview.domain.ai.support.TestTransactionManager());

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.analyzeProduct("p-1", null, null))
                .isInstanceOf(com.example.fireview.global.exception.CustomException.class);
        assertThat(allStarted.getCount()).as("세 호출이 모두 시작되어야 한다").isZero();
        assertThat(threadNames)
                .as("호출은 주입된 executor 스레드에서 실행되어야 한다")
                .hasSize(3)
                .allMatch(name -> name.startsWith("ai-call-test-"));
    }

    private static Object waitForOthers(CountDownLatch latch, Set<String> threadNames) throws InterruptedException {
        threadNames.add(Thread.currentThread().getName());
        latch.countDown();
        boolean released = latch.await(3, TimeUnit.SECONDS);
        if (!released) {
            throw new IllegalStateException("다른 호출이 시작되지 않음 — 순차 실행으로 의심됨");
        }
        return null;
    }
}

package com.example.fireview.domain.ai.service;

import com.example.fireview.domain.ai.client.AiServerClient;
import com.example.fireview.domain.ai.dto.request.AiAnalyzeRequest;
import com.example.fireview.domain.ai.dto.response.AiProductDetailResponse;
import com.example.fireview.domain.ai.dto.response.AiProductListResponse;
import com.example.fireview.domain.ai.dto.response.AiProductSummary;
import com.example.fireview.domain.ai.dto.response.AiRtiTrendResponse;
import com.example.fireview.domain.ai.dto.response.ProductAnalysisResponse;
import com.example.fireview.domain.product.repository.ProductRepository;
import com.example.fireview.domain.review.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.CompletableFuture;

/**
 * AI 분석 서비스
 *
 * 명세서 v11.0 의 트리거 원칙을 따른다:
 * 백엔드는 product_id 만 AI 서버에 전송하고 AI 서버가 자체 크롤링/연산을 수행한다.
 *
 * 흐름:
 * 1. 프론트엔드로부터 productId(외부 ID 또는 내부 DB ID) 수신
 * 2. TriggerRequest 구성 (product_id + url)
 * 3. AI 서버 3개 API 순차 호출 (product-list, product-detail, rti-trend)
 * 4. 분석 결과로 DB 업데이트 (선택)
 * 5. 프론트엔드에 통합 결과 반환
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiAnalysisService {

    private final AiServerClient aiServerClient;
    private final ProductRepository productRepository;
    private final ReviewRepository reviewRepository;

    /**
     * 상품 ID 로 AI 분석 실행.
     *
     * @param productId  분석 대상 상품 식별자 (네이버 외부 ID 또는 내부 DB ID)
     * @param productUrl 상품 페이지 URL (선택, AI 서버 크롤링 보조)
     * @return 프론트엔드에 전달할 통합 분석 결과
     */
    @Transactional
    public ProductAnalysisResponse analyzeProduct(String productId, String productUrl) {
        log.info("[AI Analysis] 분석 시작: productId={}, productUrl={}", productId, productUrl);

        AiAnalyzeRequest request = AiAnalyzeRequest.of(productId, productUrl);

        // AI 서버 3개 API 병렬 호출 (순차 호출 대비 응답시간 ~3배 단축)
        CompletableFuture<AiProductListResponse> listFuture =
                CompletableFuture.supplyAsync(() -> safeCall(() -> aiServerClient.analyzeProductList(request), "product-list"));
        CompletableFuture<AiProductDetailResponse> detailFuture =
                CompletableFuture.supplyAsync(() -> safeCall(() -> aiServerClient.analyzeProductDetail(request), "product-detail"));
        CompletableFuture<AiRtiTrendResponse> trendFuture =
                CompletableFuture.supplyAsync(() -> safeCall(() -> aiServerClient.analyzeRtiTrend(request), "rti-trend"));

        CompletableFuture.allOf(listFuture, detailFuture, trendFuture).join();

        AiProductListResponse listResponse = listFuture.join();
        AiProductDetailResponse detailResponse = detailFuture.join();
        AiRtiTrendResponse trendResponse = trendFuture.join();

        // AI 분석 결과로 DB 동기화
        if (detailResponse != null && detailResponse.results() != null) {
            updateReviewRtiScores(detailResponse);
        }
        if (listResponse != null && listResponse.products() != null && !listResponse.products().isEmpty()) {
            updateProductAvgRti(productId, listResponse.products().get(0));
        }

        log.info("[AI Analysis] 분석 완료: productId={}", productId);

        return ProductAnalysisResponse.of(productId, listResponse, detailResponse, trendResponse);
    }

    /**
     * AI 서버 호출 시 발생하는 예외를 흡수하고 null 을 반환한다.
     * 한 API 가 실패해도 나머지 API 결과는 그대로 사용한다.
     */
    private <T> T safeCall(java.util.function.Supplier<T> call, String label) {
        try {
            return call.get();
        } catch (Exception e) {
            log.warn("[AI Analysis] {} 호출 실패 (무시하고 계속): {}", label, e.getMessage());
            return null;
        }
    }

    /**
     * AI 분석 결과로 리뷰 RTI 점수 업데이트
     */
    private void updateReviewRtiScores(AiProductDetailResponse detailResponse) {
        detailResponse.results().forEach(result -> {
            try {
                Long reviewId = Long.parseLong(result.reviewId());
                reviewRepository.findById(reviewId).ifPresent(review -> {
                    review.updateRtiScore(result.rti());
                    reviewRepository.save(review);
                    log.debug("[AI Analysis] 리뷰 RTI 업데이트: reviewId={}, rti={}", reviewId, result.rti());
                });
            } catch (NumberFormatException e) {
                log.debug("[AI Analysis] reviewId 파싱 불가: {}", result.reviewId());
            }
        });
    }

    /**
     * AI 요약 결과로 상품 평균 RTI 업데이트
     */
    private void updateProductAvgRti(String productId, AiProductSummary summary) {
        try {
            Long internalProductId = Long.parseLong(productId);
            productRepository.findById(internalProductId).ifPresent(product -> {
                product.updateAvgRti(summary.averageRti());
                product.updateReviewCount(summary.reviewCount());
                productRepository.save(product);
                log.info("[AI Analysis] 상품 avgRti 업데이트: productId={}, avgRti={}, reviewCount={}",
                        productId, summary.averageRti(), summary.reviewCount());
            });
        } catch (NumberFormatException e) {
            log.debug("[AI Analysis] 내부 productId 파싱 불가: {}", productId);
        }
    }
}

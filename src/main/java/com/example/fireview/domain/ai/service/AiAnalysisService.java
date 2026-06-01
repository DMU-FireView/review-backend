package com.example.fireview.domain.ai.service;

import com.example.fireview.domain.ai.client.AiServerClient;
import com.example.fireview.domain.ai.dto.request.AiAnalyzeRequest;
import com.example.fireview.domain.ai.dto.response.AiProductDetailResponse;
import com.example.fireview.domain.ai.dto.response.AiProductListResponse;
import com.example.fireview.domain.ai.dto.response.AiProductRiskReportResponse;
import com.example.fireview.domain.ai.dto.response.AiProductSummary;
import com.example.fireview.domain.ai.dto.response.AiRtiTrendResponse;
import com.example.fireview.domain.ai.dto.response.ProductAnalysisResponse;
import com.example.fireview.domain.ai.dto.response.SampleReview;
import com.example.fireview.domain.product.entity.Product;
import com.example.fireview.domain.product.repository.ProductRepository;
import com.example.fireview.domain.review.entity.Review;
import com.example.fireview.domain.review.entity.TrustGrade;
import com.example.fireview.domain.review.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
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

        // risk-report 호출 → 리뷰 내용 포함 (sample_reviews)
        AiProductRiskReportResponse riskReport =
                safeCall(() -> aiServerClient.analyzeProductRiskReport(request), "risk-report");

        // AI 분석 결과로 DB 동기화
        if (riskReport != null && riskReport.sampleReviews() != null && !riskReport.sampleReviews().isEmpty()) {
            syncReviewsFromRiskReport(productId, riskReport.sampleReviews());
        } else if (detailResponse != null && detailResponse.results() != null) {
            updateReviewRtiScores(detailResponse);
        }
        if (listResponse != null && listResponse.products() != null && !listResponse.products().isEmpty()) {
            updateProductAvgRti(productId, listResponse.products().get(0));
        }

        log.info("[AI Analysis] 분석 완료: productId={}", productId);

        return ProductAnalysisResponse.of(productId, listResponse, detailResponse, trendResponse, riskReport);
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
     * risk-report의 sample_reviews를 Spring DB reviews 테이블에 저장.
     * 이미 존재하는 리뷰는 RTI 점수만 업데이트하고 중복 저장하지 않음.
     */
    private void syncReviewsFromRiskReport(String naverProductId, List<SampleReview> sampleReviews) {
        Product product = productRepository.findByNaverProductId(naverProductId)
                .or(() -> {
                    try { return productRepository.findById(Long.parseLong(naverProductId)); }
                    catch (NumberFormatException e) { return java.util.Optional.empty(); }
                })
                .orElse(null);

        if (product == null) {
            log.warn("[AI Analysis] 리뷰 동기화 실패 - 상품 없음: naverProductId={}", naverProductId);
            return;
        }

        final Product finalProduct = product;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd");

        for (SampleReview sr : sampleReviews) {
            try {
                Long reviewId = Long.parseLong(sr.review_id());
                if (reviewRepository.existsById(reviewId)) {
                    reviewRepository.findById(reviewId).ifPresent(r -> {
                        r.updateRtiScore(0); // level로 재계산
                        reviewRepository.save(r);
                    });
                    continue;
                }

                LocalDateTime writtenAt;
                try {
                    writtenAt = java.time.LocalDate.parse(sr.date(), formatter).atStartOfDay();
                } catch (DateTimeParseException ex) {
                    writtenAt = LocalDateTime.now();
                }

                List<String> reasonMessages = sr.reasons() != null
                        ? sr.reasons().stream().map(r -> r.message()).filter(m -> m != null && !m.isBlank()).toList()
                        : List.of();

                int rti = switch (sr.level()) { case "safe" -> 85; case "danger" -> 30; default -> 55; };
                TrustGrade grade = TrustGrade.fromRti(rti);

                Review review = Review.builder()
                        .id(reviewId)
                        .product(finalProduct)
                        .reviewerNickname(sr.author() != null ? sr.author() : "익명")
                        .reviewerId(sr.author() != null ? sr.author() : "unknown")
                        .content(sr.content() != null ? sr.content() : "")
                        .rating(sr.rating() != null ? sr.rating() : 0)
                        .rtiScore((double) rti)
                        .trustGrade(grade)
                        .reasons(reasonMessages)
                        .writtenAt(writtenAt)
                        .isVerifiedPurchase(false)
                        .build();
                reviewRepository.save(review);
                log.info("[AI Analysis] 리뷰 저장: reviewId={}, product={}", reviewId, naverProductId);
            } catch (Exception e) {
                log.warn("[AI Analysis] 리뷰 저장 실패: {}", e.getMessage());
            }
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
     * AI 요약 결과로 상품 평균 RTI 업데이트.
     * productId는 네이버 쇼핑 API의 외부 ID이므로 naverProductId 컬럼으로 조회한다.
     */
    private void updateProductAvgRti(String productId, AiProductSummary summary) {
        productRepository.findByNaverProductId(productId).ifPresentOrElse(product -> {
            product.updateAvgRti(summary.averageRti());
            product.updateReviewCount(summary.reviewCount());
            productRepository.save(product);
            log.info("[AI Analysis] 상품 avgRti 업데이트: naverProductId={}, avgRti={}, reviewCount={}",
                    productId, summary.averageRti(), summary.reviewCount());
        }, () -> log.debug("[AI Analysis] DB에서 상품을 찾을 수 없음 (naverProductId={})", productId));
    }
}

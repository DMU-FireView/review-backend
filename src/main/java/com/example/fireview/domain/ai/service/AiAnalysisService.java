package com.example.fireview.domain.ai.service;

import com.example.fireview.domain.ai.client.AiServerClient;
import com.example.fireview.domain.ai.dto.request.AiAnalyzeRequest;
import com.example.fireview.domain.ai.dto.request.AiReviewItem;
import com.example.fireview.domain.ai.dto.response.*;
import com.example.fireview.domain.product.entity.Product;
import com.example.fireview.domain.product.repository.ProductRepository;
import com.example.fireview.domain.review.entity.Review;
import com.example.fireview.domain.review.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * AI 분석 서비스
 *
 * 흐름:
 * 1. 프론트엔드로부터 productId(네이버 상품 ID) 수신
 * 2. DB에 저장된 리뷰 데이터를 AI 서버 형식으로 변환
 * 3. AI 서버 3개 API 순차 호출 (product-detail, product-list, rti-trend)
 * 4. AI 분석 결과로 리뷰 RTI 점수 업데이트
 * 5. 프론트엔드에 통합 결과 반환
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiAnalysisService {

    private final AiServerClient aiServerClient;
    private final ProductRepository productRepository;
    private final ReviewRepository reviewRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * 상품 ID로 AI 분석 실행
     *
     * @param productId 네이버 상품 ID (예: "7195971829")
     * @return 프론트엔드에 전달할 통합 분석 결과
     */
    @Transactional
    public ProductAnalysisResponse analyzeProduct(String productId) {
        log.info("[AI Analysis] 분석 시작: productId={}", productId);

        // 1. DB에서 해당 상품의 리뷰 조회 (product.platform 이나 이름으로 매칭되는 상품 먼저 탐색)
        //    리뷰가 없으면 빈 배열로 AI 서버에 요청 (AI 서버가 product_id 기반으로 크롤링)
        List<AiReviewItem> reviewItems = buildReviewItems(productId);

        AiAnalyzeRequest request = new AiAnalyzeRequest(reviewItems);
        log.info("[AI Analysis] AI 서버 요청 준비 완료: 리뷰 수={}", reviewItems.size());

        // 2. AI 서버 3개 API 호출
        AiProductDetailResponse detailResponse = null;
        AiProductListResponse listResponse = null;
        AiRtiTrendResponse trendResponse = null;

        try {
            detailResponse = aiServerClient.analyzeProductDetail(request);
        } catch (Exception e) {
            log.warn("[AI Analysis] product-detail 호출 실패 (무시하고 계속): {}", e.getMessage());
        }

        try {
            listResponse = aiServerClient.analyzeProductList(request);
        } catch (Exception e) {
            log.warn("[AI Analysis] product-list 호출 실패 (무시하고 계속): {}", e.getMessage());
        }

        try {
            trendResponse = aiServerClient.analyzeRtiTrend(request);
        } catch (Exception e) {
            log.warn("[AI Analysis] rti-trend 호출 실패 (무시하고 계속): {}", e.getMessage());
        }

        // 3. AI 분석 결과로 DB 업데이트
        if (detailResponse != null && detailResponse.results() != null) {
            updateReviewRtiScores(detailResponse);
        }

        if (listResponse != null && listResponse.products() != null && !listResponse.products().isEmpty()) {
            updateProductAvgRti(productId, listResponse.products().get(0));
        }

        log.info("[AI Analysis] 분석 완료: productId={}", productId);

        // 4. 통합 결과 반환
        return ProductAnalysisResponse.of(productId, listResponse, detailResponse, trendResponse);
    }

    /**
     * DB에 저장된 리뷰를 AI 서버 요청 형식으로 변환
     * 리뷰가 없는 경우 빈 배열 반환 (AI 서버가 product_id로 직접 크롤링)
     */
    private List<AiReviewItem> buildReviewItems(String productId) {
        // productId가 숫자(Long)인 경우 내부 DB ID로 조회
        try {
            Long internalProductId = Long.parseLong(productId);
            Product product = productRepository.findById(internalProductId).orElse(null);

            if (product != null) {
                List<Review> reviews = reviewRepository.findByProduct(product);
                if (!reviews.isEmpty()) {
                    log.info("[AI Analysis] DB 리뷰 {}건 발견, AI 서버에 전송", reviews.size());
                    return reviews.stream()
                            .map(r -> new AiReviewItem(
                                    String.valueOf(r.getId()),
                                    productId,
                                    r.getContent(),
                                    r.getReviewerId() != null ? r.getReviewerId() : "unknown",
                                    r.getRating(),
                                    r.getWrittenAt() != null
                                            ? r.getWrittenAt().toLocalDate().format(DATE_FORMATTER)
                                            : "2026-01-01",
                                    0,
                                    null
                            ))
                            .toList();
                }
            }
        } catch (NumberFormatException e) {
            // productId가 네이버 외부 ID인 경우 (숫자가 아닐 수도 있음)
            log.debug("[AI Analysis] productId가 내부 ID가 아님: {}", productId);
        }

        // 리뷰가 없으면 빈 배열 반환 → AI 서버가 product_id 기반으로 크롤링 처리
        log.info("[AI Analysis] DB 리뷰 없음. AI 서버에 빈 배열 전송 (AI 서버가 크롤링 담당)");
        return List.of();
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

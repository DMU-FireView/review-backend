package com.example.fireview.domain.ai.client;

import com.example.fireview.domain.ai.dto.request.AiAnalyzeRequest;
import com.example.fireview.domain.ai.dto.response.AiProductDetailResponse;
import com.example.fireview.domain.ai.dto.response.AiProductListResponse;
import com.example.fireview.domain.ai.dto.response.AiProductRiskReportResponse;
import com.example.fireview.domain.ai.dto.response.AiReviewReportResponse;
import com.example.fireview.domain.ai.dto.response.AiRtiTrendResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * AI 서버 HTTP 클라이언트
 *
 * 명세서 v11.0 의 5개 AI 분석 API 를 호출한다.
 * 모든 엔드포인트는 공통 TriggerRequest (product_id + 선택 url) 만 받아서
 * AI 서버가 자체 크롤링을 수행한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiServerClient {

    private final RestTemplate restTemplate;

    @Value("${ai.server.base-url}")
    private String baseUrl;

    /**
     * 1. 상품 목록 RTI 요약
     * POST /api/internal/ai/products/product-list
     */
    public AiProductListResponse analyzeProductList(AiAnalyzeRequest request) {
        return post("/api/internal/ai/products/product-list", request, AiProductListResponse.class, "product-list");
    }

    /**
     * 2. 개별 리뷰 상세 분석
     * POST /api/internal/ai/reviews/product-detail
     */
    public AiProductDetailResponse analyzeProductDetail(AiAnalyzeRequest request) {
        return post("/api/internal/ai/reviews/product-detail", request, AiProductDetailResponse.class, "product-detail");
    }

    /**
     * 3. RTI 30일 추이 집계
     * POST /api/internal/ai/products/rti-trend
     */
    public AiRtiTrendResponse analyzeRtiTrend(AiAnalyzeRequest request) {
        return post("/api/internal/ai/products/rti-trend", request, AiRtiTrendResponse.class, "rti-trend");
    }

    /**
     * 4. 리뷰 상세 분석 리포트
     * POST /api/internal/ai/reviews/report
     */
    public AiReviewReportResponse analyzeReviewReport(AiAnalyzeRequest request) {
        return post("/api/internal/ai/reviews/report", request, AiReviewReportResponse.class, "review-report");
    }

    /**
     * 5. 상품 위험도 리포트
     * POST /api/internal/ai/products/risk-report
     */
    public AiProductRiskReportResponse analyzeProductRiskReport(AiAnalyzeRequest request) {
        return post("/api/internal/ai/products/risk-report", request, AiProductRiskReportResponse.class, "risk-report");
    }

    // ─────────────────────────────────────────────────────────────

    private <T> T post(String path, AiAnalyzeRequest request, Class<T> responseType, String label) {
        String url = baseUrl + path;
        log.info("[AI Client] {} 요청: productId={}", label, request.productId());

        try {
            T response = restTemplate.postForObject(url, buildHttpEntity(request), responseType);
            log.info("[AI Client] {} 응답 수신", label);
            return response;
        } catch (RestClientException e) {
            log.error("[AI Client] {} 호출 실패: {}", label, e.getMessage());
            throw new RuntimeException("AI 서버 " + label + " 호출 실패: " + e.getMessage(), e);
        }
    }

    private HttpEntity<AiAnalyzeRequest> buildHttpEntity(AiAnalyzeRequest request) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(request, headers);
    }
}

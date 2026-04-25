package com.example.fireview.domain.ai.client;

import com.example.fireview.domain.ai.dto.request.AiAnalyzeRequest;
import com.example.fireview.domain.ai.dto.response.AiProductDetailResponse;
import com.example.fireview.domain.ai.dto.response.AiProductListResponse;
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
 * 3개 AI 분석 API를 호출합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiServerClient {

    private final RestTemplate restTemplate;

    @Value("${ai.server.base-url}")
    private String baseUrl;

    /**
     * 개별 리뷰 상세 분석 API 호출
     * POST /api/internal/ai/reviews/product-detail
     */
    public AiProductDetailResponse analyzeProductDetail(AiAnalyzeRequest request) {
        String url = baseUrl + "/api/internal/ai/reviews/product-detail";
        log.info("[AI Client] product-detail 요청: productId={}, 리뷰 수={}",
                extractProductId(request), request.reviews().size());

        try {
            AiProductDetailResponse response = restTemplate.postForObject(
                    url, buildHttpEntity(request), AiProductDetailResponse.class);
            log.info("[AI Client] product-detail 응답 수신: 결과 수={}",
                    response != null && response.results() != null ? response.results().size() : 0);
            return response;
        } catch (RestClientException e) {
            log.error("[AI Client] product-detail 호출 실패: {}", e.getMessage());
            throw new RuntimeException("AI 서버 product-detail 호출 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 상품 목록 RTI 요약 API 호출
     * POST /api/internal/ai/products/product-list
     */
    public AiProductListResponse analyzeProductList(AiAnalyzeRequest request) {
        String url = baseUrl + "/api/internal/ai/products/product-list";
        log.info("[AI Client] product-list 요청: productId={}", extractProductId(request));

        try {
            AiProductListResponse response = restTemplate.postForObject(
                    url, buildHttpEntity(request), AiProductListResponse.class);
            log.info("[AI Client] product-list 응답 수신: 상품 수={}",
                    response != null && response.products() != null ? response.products().size() : 0);
            return response;
        } catch (RestClientException e) {
            log.error("[AI Client] product-list 호출 실패: {}", e.getMessage());
            throw new RuntimeException("AI 서버 product-list 호출 실패: " + e.getMessage(), e);
        }
    }

    /**
     * RTI 추이 그래프 API 호출
     * POST /api/internal/ai/products/rti-trend
     */
    public AiRtiTrendResponse analyzeRtiTrend(AiAnalyzeRequest request) {
        String url = baseUrl + "/api/internal/ai/products/rti-trend";
        log.info("[AI Client] rti-trend 요청: productId={}", extractProductId(request));

        try {
            AiRtiTrendResponse response = restTemplate.postForObject(
                    url, buildHttpEntity(request), AiRtiTrendResponse.class);
            log.info("[AI Client] rti-trend 응답 수신: 추이 건수={}",
                    response != null && response.trend() != null ? response.trend().size() : 0);
            return response;
        } catch (RestClientException e) {
            log.error("[AI Client] rti-trend 호출 실패: {}", e.getMessage());
            throw new RuntimeException("AI 서버 rti-trend 호출 실패: " + e.getMessage(), e);
        }
    }

    private HttpEntity<AiAnalyzeRequest> buildHttpEntity(AiAnalyzeRequest request) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(request, headers);
    }

    private String extractProductId(AiAnalyzeRequest request) {
        if (request.reviews() != null && !request.reviews().isEmpty()) {
            return request.reviews().get(0).productId();
        }
        return "unknown";
    }
}

package com.example.fireview.domain.ai.dto.response;

import java.util.List;

/**
 * 백엔드 → 프론트엔드 분석 결과 응답 DTO
 * AI 서버의 3가지 분석 결과를 통합해서 반환합니다.
 */
public record ProductAnalysisResponse(

        String productId,

        // product-list 요약
        Double averageRti,
        String level,
        Integer reviewCount,
        Integer safeCount,
        Integer warnCount,
        Integer dangerCount,

        // product-detail 개별 리뷰 분석 결과
        List<AiAnalysisResult> reviews,

        // rti-trend 날짜별 추이
        List<AiTrendEntry> trend

) {
    public static ProductAnalysisResponse of(
            String productId,
            AiProductListResponse listResponse,
            AiProductDetailResponse detailResponse,
            AiRtiTrendResponse trendResponse
    ) {
        AiProductSummary summary = (listResponse != null && !listResponse.products().isEmpty())
                ? listResponse.products().get(0)
                : null;

        return new ProductAnalysisResponse(
                productId,
                summary != null ? summary.averageRti() : null,
                summary != null ? summary.level() : null,
                summary != null ? summary.reviewCount() : null,
                summary != null ? summary.safeCount() : null,
                summary != null ? summary.warnCount() : null,
                summary != null ? summary.dangerCount() : null,
                detailResponse != null ? detailResponse.results() : List.of(),
                trendResponse != null ? trendResponse.trend() : List.of()
        );
    }
}

package com.example.fireview.domain.ai.dto.response;

import java.util.List;

/**
 * 백엔드 → 프론트엔드 분석 결과 응답 DTO
 * AI 서버의 3가지 분석 결과를 통합해서 반환합니다.
 *
 * - AI 내부 필드(input_features 등) 노출 없음
 * - 모든 필드 null-safe 기본값 보장
 */
public record ProductAnalysisResponse(

        String productId,

        // product-list 요약
        Double averageRti,     // null 시 기본값 50.0
        String level,          // safe | warn | danger (null 시 "warn")
        Integer reviewCount,   // null 시 0
        Integer safeCount,
        Integer warnCount,
        Integer dangerCount,

        // product-detail 개별 리뷰 분석 결과 (AI 내부 필드 제거된 정제 DTO)
        List<ReviewAnalysisDto> reviews,

        // rti-trend 날짜별 추이
        List<TrendEntryDto> trend

) {
    public static ProductAnalysisResponse of(
            String productId,
            AiProductListResponse listResponse,
            AiProductDetailResponse detailResponse,
            AiRtiTrendResponse trendResponse,
            AiProductRiskReportResponse riskReport
    ) {
        AiProductSummary summary = (listResponse != null
                && listResponse.products() != null
                && !listResponse.products().isEmpty())
                ? listResponse.products().get(0)
                : null;

        // risk-report sample_reviews와 product-detail 결과를 review_id 기준으로 병합
        // → content, author, date + rti, signals 모두 포함
        List<ReviewAnalysisDto> reviewDtos;
        if (riskReport != null && riskReport.sampleReviews() != null && !riskReport.sampleReviews().isEmpty()) {
            java.util.Map<String, AiAnalysisResult> detailMap = (detailResponse != null && detailResponse.results() != null)
                    ? detailResponse.results().stream()
                            .collect(java.util.stream.Collectors.toMap(AiAnalysisResult::reviewId, r -> r, (a, b) -> a))
                    : java.util.Map.of();

            reviewDtos = riskReport.sampleReviews().stream()
                    .map(sample -> ReviewAnalysisDto.from(sample, detailMap.get(sample.review_id())))
                    .toList();
        } else if (detailResponse != null && detailResponse.results() != null) {
            reviewDtos = detailResponse.results().stream()
                    .map(ReviewAnalysisDto::from)
                    .toList();
        } else {
            reviewDtos = List.of();
        }

        // 추이 데이터
        List<TrendEntryDto> trendDtos = (trendResponse != null && trendResponse.trend() != null)
                ? trendResponse.trend().stream().map(TrendEntryDto::from).toList()
                : List.of();

        return new ProductAnalysisResponse(
                productId,
                summary != null ? summary.averageRti() : 50.0,
                summary != null ? summary.level() : "warn",
                summary != null ? summary.reviewCount() : 0,
                summary != null ? summary.safeCount() : 0,
                summary != null ? summary.warnCount() : 0,
                summary != null ? summary.dangerCount() : 0,
                reviewDtos,
                trendDtos
        );
    }
}

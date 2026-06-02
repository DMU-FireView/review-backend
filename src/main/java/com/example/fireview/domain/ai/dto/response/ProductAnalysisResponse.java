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
        List<TrendEntryDto> trend,

        // 비율 통계 (AI 서버 미제공 → 백엔드 계산)
        Double realReviewRatio,      // 실사용 리뷰 비율 (safe / total)
        Double adSuspicionRatio,     // 광고성 의심 비율 (AD 관련 코드 보유 리뷰 / total)
        Double repetitiveRatio,      // 반복 표현 비율 (REPETITIVE_KEYWORD 보유 리뷰 / total)

        // 주요 판단 신호 (AI 서버 미제공 → 백엔드 계산)
        List<TrustSignalDto> trustSignals

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
                    .map(sample -> ReviewAnalysisDto.from(sample, detailMap.get(sample.reviewId())))
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

        // 비율 계산 (product-detail 전체 리뷰 기준)
        java.util.Set<String> adCodes = java.util.Set.of(
                "PURCHASE_NOT_VERIFIED", "MULTIPLE_REVIEWS_SAME_DAY",
                "SIMILAR_REVIEW_CLUSTER", "SIMILAR_REVIEW_PATTERN", "SIMILAR_REVIEW_NETWORK"
        );
        int safeCountVal = summary != null ? summary.safeCount() : 0;
        int totalVal = summary != null ? summary.reviewCount() : 0;
        double realReviewRatio = totalVal > 0 ? Math.round(safeCountVal * 1000.0 / totalVal) / 10.0 : 0.0;

        double adSuspicionRatio = 0.0;
        double repetitiveRatio = 0.0;
        List<TrustSignalDto> trustSignals = List.of();

        if (detailResponse != null && detailResponse.results() != null && !detailResponse.results().isEmpty()) {
            int total = detailResponse.results().size();

            // 광고성 의심 비율
            long adCount = detailResponse.results().stream()
                    .filter(r -> r.reasons() != null && r.reasons().stream().anyMatch(reason -> adCodes.contains(reason.code())))
                    .count();
            // 반복 표현 비율
            long repCount = detailResponse.results().stream()
                    .filter(r -> r.reasons() != null && r.reasons().stream().anyMatch(reason -> "REPETITIVE_KEYWORD".equals(reason.code())))
                    .count();
            // 구매인증 비율 (input_features.verified_purchase == "True")
            long verifiedCount = detailResponse.results().stream()
                    .filter(r -> r.inputFeatures() != null && "True".equals(String.valueOf(r.inputFeatures().get("verified_purchase"))))
                    .count();
            // 작성 시점 패턴 (MULTIPLE_REVIEWS_SAME_DAY 포함 리뷰 비율)
            long timingCount = detailResponse.results().stream()
                    .filter(r -> r.reasons() != null && r.reasons().stream().anyMatch(reason -> "MULTIPLE_REVIEWS_SAME_DAY".equals(reason.code())))
                    .count();

            adSuspicionRatio = Math.round(adCount * 1000.0 / total) / 10.0;
            repetitiveRatio  = Math.round(repCount * 1000.0 / total) / 10.0;

            double verifiedRatio = (double) verifiedCount / total;
            double repRatio      = (double) repCount / total;
            double timingRatio   = (double) timingCount / total;

            trustSignals = List.of(
                    TrustSignalDto.ofVerifiedPurchase(verifiedRatio),
                    TrustSignalDto.ofTextDiversity(1.0 - repRatio),
                    TrustSignalDto.ofRepetitivePattern(repRatio),
                    TrustSignalDto.ofTimingPattern(timingRatio)
            );
        }

        return new ProductAnalysisResponse(
                productId,
                summary != null ? summary.averageRti() : 50.0,
                summary != null ? summary.level() : "warn",
                summary != null ? summary.reviewCount() : 0,
                summary != null ? summary.safeCount() : 0,
                summary != null ? summary.warnCount() : 0,
                summary != null ? summary.dangerCount() : 0,
                reviewDtos,
                trendDtos,
                realReviewRatio,
                adSuspicionRatio,
                repetitiveRatio,
                trustSignals
        );
    }
}

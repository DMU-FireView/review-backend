package com.example.fireview.domain.ai.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * 상품 위험도 리포트 응답 (명세서 v11.0 §3.5)
 *
 * AI 서버 엔드포인트: POST /api/internal/ai/products/risk-report
 *
 * MVP 스펙에서 patterns 필드가 제외되고, SampleReview 의 tags 가 reasons 로 대체됨.
 */
public record AiProductRiskReportResponse(

        @JsonProperty("product_id")
        String productId,

        @JsonProperty("product_name")
        String productName,

        @JsonProperty("summary_stat")
        SummaryStat summaryStat,

        List<AiTrendEntry> trend,

        @JsonProperty("sample_reviews")
        List<SampleReview> sampleReviews

) {}

package com.example.fireview.domain.ai.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * AI 서버 상품 단위 요약 통계 (product-list 응답 내 items)
 */
public record AiProductSummary(

        @JsonProperty("product_id")
        String productId,

        @JsonProperty("average_rti")
        Double averageRti,

        String level,           // safe | warn | danger

        @JsonProperty("review_count")
        Integer reviewCount,

        @JsonProperty("safe_count")
        Integer safeCount,

        @JsonProperty("warn_count")
        Integer warnCount,

        @JsonProperty("danger_count")
        Integer dangerCount

) {}

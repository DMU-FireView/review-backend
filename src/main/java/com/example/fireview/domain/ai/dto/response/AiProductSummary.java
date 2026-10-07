package com.example.fireview.domain.ai.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 레거시: 새 구조에서는 Data 서버가 ai.re-view.kr 를 호출. 이 경로는 프론트 옛 상세 화면 호환용.
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

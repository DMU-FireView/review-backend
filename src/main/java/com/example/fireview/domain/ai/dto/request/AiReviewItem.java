package com.example.fireview.domain.ai.dto.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * AI 서버에 전달하는 개별 리뷰 항목 (8개 필드)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AiReviewItem(

        @JsonProperty("review_id")
        String reviewId,

        @JsonProperty("product_id")
        String productId,

        String content,

        @JsonProperty("user_id")
        String userId,

        Integer rating,

        @JsonProperty("review_date")
        String reviewDate,

        @JsonProperty("image_count")
        Integer imageCount,

        @JsonProperty("quality_score")
        Double qualityScore

) {}

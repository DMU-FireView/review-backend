package com.example.fireview.domain.dataserver.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Data 서버 상품 응답 (GET /api/v1/{platform}/products/{product_id} 의 product).
 *
 * <p>분석 결과(RTI·등급·사유)는 들어 있지 않다. Data 서버는 원본 수집만 소유한다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record DataServerProduct(
        String platform,
        @JsonProperty("product_id") String productId,
        String name,
        String url,
        String brand,
        String manufacturer,
        String seller,
        Integer price,
        @JsonProperty("thumbnail_url") String thumbnailUrl,
        String category,
        @JsonProperty("review_count") Integer reviewCount,
        Double rating,
        @JsonProperty("last_collected_at") String lastCollectedAt
) {}

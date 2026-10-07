package com.example.fireview.domain.dataserver.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Data 서버 상품 응답 (GET /api/v1/{platform}/products/{product_id} 의 product).
 *
 * <p>상품 정보만 담는다. 신뢰도 분석 결과는 같은 응답의 최상위 {@code analysis} 로 따로 온다
 * ({@link DataServerAnalysis}).
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

package com.example.fireview.domain.search.dto;

import com.example.fireview.domain.product.dto.ProductResponse;

import java.util.List;

/**
 * 네이버 쇼핑 검색 API 응답 래퍼 DTO
 */
public record NaverSearchResponse(
        String keyword,
        int totalCount,
        List<ProductResponse> products
) {
    public static NaverSearchResponse of(String keyword, List<ProductResponse> products) {
        return new NaverSearchResponse(keyword, products.size(), products);
    }
}

package com.example.fireview.domain.product.client;

/**
 * 네이버 쇼핑 검색 API 응답의 items[] 개별 항목.
 *
 * API 명세: GET https://openapi.naver.com/v1/search/shop.json
 * {
 *   "items": [
 *     {
 *       "title":       "상품명 (HTML 태그 포함)",
 *       "link":        "상품 페이지 URL",
 *       "image":       "썸네일 URL",
 *       "lprice":      "최저가 (원)",
 *       "mallName":    "판매처 이름",
 *       "productId":   "네이버 상품 ID",
 *       "brand":       "브랜드",
 *       "category1":   "대분류 (예: 디지털/가전)",
 *       "category2":   "중분류",
 *       "category3":   "소분류",
 *       "category4":   "세분류"
 *     }
 *   ]
 * }
 */
public record NaverShoppingItem(
        String title,
        String link,
        String image,
        String lprice,
        String mallName,
        String productId,
        String brand,
        String category1,
        String category2,
        String category3
) {}

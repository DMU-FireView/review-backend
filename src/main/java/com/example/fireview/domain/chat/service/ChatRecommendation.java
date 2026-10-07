package com.example.fireview.domain.chat.service;

import com.example.fireview.domain.product.entity.Product;

/**
 * 챗봇 답변 아래에 붙는 비슷한 상품 하나.
 *
 * <p>값을 모르면 null 이다. 번호표를 만들 때 리뷰 수·평점이 비어 있으면 0 으로 채워지는데
 * (Product.onCreate) 그 0 은 "모름"이라 그대로 내보내면 화면에 0개·0.0점으로 보인다.
 * 그래서 0 이하는 null 로 바꾼다.
 *
 * @param externalId  {@code "{platform}-{productId}"}. 챗봇 productId 에 그대로 넣을 수 있다
 * @param platform    Data 서버 수집기 이름 (예: kurly)
 * @param productId   쇼핑몰 상품 ID
 * @param price       가격(원). 모르면 null
 * @param reviewCount 리뷰 수. 모르면 null
 * @param rating      평균 평점. 모르면 null
 */
public record ChatRecommendation(String externalId, String platform, String productId,
                                 String name, Long price, String thumbnailUrl,
                                 Integer reviewCount, Double rating) {

    public static ChatRecommendation from(Product product) {
        return new ChatRecommendation(
                product.dataServerExternalId(),
                product.getDataPlatform(),
                product.getDataProductId(),
                product.getName(),
                positiveOrNull(product.getPrice()),
                product.getImageUrl(),
                product.getReviewCount() == null || product.getReviewCount() <= 0
                        ? null : product.getReviewCount(),
                product.getAvgRating() == null || product.getAvgRating() <= 0
                        ? null : product.getAvgRating());
    }

    private static Long positiveOrNull(Long value) {
        return value == null || value <= 0 ? null : value;
    }
}

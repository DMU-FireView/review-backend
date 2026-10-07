package com.example.fireview.domain.chat.dto.response;

import com.example.fireview.domain.chat.service.ChatRecommendation;

/**
 * 챗봇 답변 아래에 보여줄 추천 상품 카드.
 *
 * <p>카드를 누르면 {@code /product/:platform/:productId} 로 연다.
 * 값을 모르면 null 이다. 0 으로 채우지 않는다.
 *
 * @param externalId  {@code "{platform}-{productId}"}. 이 상품으로 새 대화를 열 때 productId 로 쓴다
 * @param platform    Data 서버 수집기 이름 (예: kurly)
 * @param productId   쇼핑몰 상품 ID
 * @param name        상품명
 * @param price       가격(원). 모르면 null
 * @param thumbnailUrl 대표 이미지. 없으면 null
 * @param reviewCount 리뷰 수. 모르면 null (표시하지 않는다)
 * @param rating      평균 평점. 모르면 null (표시하지 않는다)
 */
public record ChatRecommendationResponse(
        String externalId,
        String platform,
        String productId,
        String name,
        Long price,
        String thumbnailUrl,
        Integer reviewCount,
        Double rating
) {
    public static ChatRecommendationResponse from(ChatRecommendation recommendation) {
        return new ChatRecommendationResponse(recommendation.externalId(),
                recommendation.platform(), recommendation.productId(), recommendation.name(),
                recommendation.price(), recommendation.thumbnailUrl(),
                recommendation.reviewCount(), recommendation.rating());
    }
}

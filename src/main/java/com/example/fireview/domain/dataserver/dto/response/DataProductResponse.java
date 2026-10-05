package com.example.fireview.domain.dataserver.dto.response;

import java.util.List;

/**
 * Data 서버 기반 상품 상세 응답.
 *
 * <p>기존 {@code /api/products/{id}} 와 다른 점:
 * <ul>
 *   <li>상품의 주인이 Data 서버다. Spring 은 조합만 한다</li>
 *   <li>{@code collectionStatus} 가 있다. 데이터가 없거나 오래된 상태를 숨기지 않는다</li>
 *   <li>{@code analysis} 가 비어 있다. 신뢰도 분석은 아직 어느 서버도 제공하지 않는다</li>
 * </ul>
 *
 * @param collectionStatus 수집 신선도. {@code QUEUED} 면 {@code product} 가 null 이다
 * @param springProductId  찜·장바구니에 쓸 Spring 쪽 상품 번호. **아직 없으면 null**
 * @param product          상품 정보. {@code QUEUED}·{@code UNAVAILABLE} 이면 null
 * @param reviews          리뷰 한 페이지
 * @param job              수집 job. 진행 중일 때만 들어온다
 * @param analysis         신뢰도 분석. 현재는 항상 null
 */
public record DataProductResponse(
        CollectionStatus collectionStatus,
        Long springProductId,
        DataProductDetail product,
        ReviewPage reviews,
        CollectionJobStatus job,
        Object analysis
) {

    /**
     * @param externalId {@code "{platform}-{productId}"}. 챗봇·알림처럼 식별자를 한 칸에
     *                   담아야 하는 자리에서 쓴다
     */
    public record DataProductDetail(
            String platform,
            String productId,
            String externalId,
            String name,
            String url,
            String brand,
            String manufacturer,
            String seller,
            Integer price,
            String thumbnailUrl,
            String category,
            Integer reviewCount,
            Double rating,
            String lastCollectedAt
    ) {}

    /**
     * @param nextCursor 다음 페이지 요청에 그대로 넣는다. null 이면 마지막 페이지
     */
    public record ReviewPage(List<DataReview> items, String nextCursor) {}

    /**
     * @param rating 5점 만점 환산. 소수점이 올 수 있다 (예: 4.5)
     */
    public record DataReview(
            String reviewId,
            String content,
            Double rating,
            String author,
            String writtenAt,
            String option,
            List<String> images,
            Integer helpfulCount
    ) {}

    /**
     * @param status        pending / running / succeeded / partial / failed
     * @param productStatus 상품 수집 결과
     * @param reviewStatus  리뷰 수집 결과. 상품만 성공한 부분 실패를 구분하려고 따로 둔다
     */
    public record CollectionJobStatus(
            Long id,
            String status,
            String productStatus,
            String reviewStatus,
            String lastError
    ) {}
}

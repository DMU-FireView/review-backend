package com.example.fireview.domain.dataserver.dto.response;

import java.util.List;

/**
 * Data 서버 기반 상품 상세 응답.
 *
 * <p>기존 {@code /api/products/{id}} 와 다른 점:
 * <ul>
 *   <li>상품의 주인이 Data 서버다. Spring 은 조합만 한다</li>
 *   <li>{@code collectionStatus} 가 있다. 데이터가 없거나 오래된 상태를 숨기지 않는다</li>
 *   <li>{@code analysis} 는 분석 상태만 담는다. 점수는 리뷰마다 {@code reviews.items[]} 에 붙는다.
 *       상품 단위 평균 RTI·등급은 아직 없다</li>
 * </ul>
 *
 * @param collectionStatus 수집 신선도. {@code QUEUED} 면 {@code product} 가 null 이다
 * @param springProductId  찜·장바구니에 쓸 Spring 쪽 상품 번호. **아직 없으면 null**
 * @param product          상품 정보. {@code QUEUED}·{@code UNAVAILABLE} 이면 null
 * @param reviews          리뷰 한 페이지
 * @param job              수집 job. 진행 중일 때만 들어온다
 * @param analysis         신뢰도 분석 상태. null 이 아니다 — 모르면 {@code UNAVAILABLE}
 */
public record DataProductResponse(
        CollectionStatus collectionStatus,
        Long springProductId,
        DataProductDetail product,
        ReviewPage reviews,
        CollectionJobStatus job,
        ProductAnalysis analysis
) {

    /**
     * 상품의 신뢰도 분석 상태.
     *
     * <p><b>null 대신 {@code UNAVAILABLE} 을 쓴다.</b> 구버전 Data 서버가 {@code analysis} 를
     * 보내지 않거나 Data 서버에 닿지 못한 경우다. null 로 두면 "분석 결과 없음"과
     * "분석 상태를 모름"이 같아 보이고, 프론트가 매번 null 분기를 따로 짜야 한다.
     * {@code collectionStatus} 의 {@code UNAVAILABLE} 과 같은 방식이다.
     *
     * <p>상품 평균 RTI·등급은 넣지 않는다. Data 서버가 주는 결과는 지금 리뷰 페이지 것뿐이라
     * Spring 이 평균을 내면 20건 표본 평균이 된다. Data 서버가 전체로 계산해 줄 때 붙인다.
     *
     * @param status        분석 상태
     * @param modelVersion  분석한 AI 모델 버전. 분석 job 이 없으면 null
     * @param policyVersion 등급 정책 버전 (예: {@code rti-v0}). 분석 job 이 없으면 null
     * @param reviewCount   분석 job 에 들어간 리뷰 전체 수. 상품 리뷰 수와 다를 수 있다. 모르면 null
     */
    public record ProductAnalysis(
            AnalysisStatus status,
            String modelVersion,
            String policyVersion,
            Integer reviewCount
    ) {
        public static ProductAnalysis unavailable() {
            return new ProductAnalysis(AnalysisStatus.UNAVAILABLE, null, null, null);
        }
    }

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
     * @param rating  5점 만점 환산. 소수점이 올 수 있다 (예: 4.5)
     * @param rti     이 리뷰의 RTI (0~100). 분석 결과가 없거나 계산 불가면 null. 0 으로 채우지 않는다
     * @param level   이 리뷰의 등급. Data 서버 값 그대로 {@code safe}/{@code warn}/{@code danger}.
     *                결과가 없거나 계산 불가면 null
     * @param reasons 판단 근거. 결과가 없으면 빈 배열
     */
    public record DataReview(
            String reviewId,
            String content,
            Double rating,
            String author,
            String writtenAt,
            String option,
            List<String> images,
            Integer helpfulCount,
            Double rti,
            String level,
            List<String> reasons
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

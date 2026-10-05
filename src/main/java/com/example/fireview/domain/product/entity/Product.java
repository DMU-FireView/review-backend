package com.example.fireview.domain.product.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Entity
@Table(name = "products",
        // 같은 Data 서버 상품이 Spring 쪽에 두 행으로 생기면 찜·장바구니가 갈라진다.
        // 두 칼럼 모두 NULL 인 기존 행끼리는 충돌하지 않는다(Postgres 는 NULL 을 서로 다르게 본다).
        uniqueConstraints = @UniqueConstraint(
                name = "uq_products_data_server_key",
                columnNames = {"data_platform", "data_product_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    private Long id;

    @Column(nullable = false)
    private String name;

    private String imageUrl;

    private Long price;

    /**
     * 카테고리. Data 서버에서 온 상품은 비어 있을 수 있다.
     *
     * <p>Data 서버의 category 는 "뷰티 인디 > 인디 스킨케어 > 인디 마스크/팩" 같은
     * 자유 문자열이라 이 enum 으로 안전하게 옮길 수 없다. 억지로 끼워 맞추면
     * 화면에 엉뚱한 분류가 뜬다. 분류가 필요해질 때 매핑을 따로 만든다.
     */
    @Enumerated(EnumType.STRING)
    private Category category;

    @Column(nullable = false)
    private String platform;

    /**
     * 상품 평균 RTI. <b>분석 전이면 null 이다.</b>
     *
     * <p>Data 서버는 원본 수집만 소유하고 신뢰도 분석을 주지 않는다. 값이 없을 때
     * 0 이나 50 같은 기본값을 넣으면 화면이 그 수치를 실제 분석 결과처럼 보여준다.
     * 모른다는 것은 모른다고 두는 편이 맞다.
     */
    private Double avgRti;

    private Integer reviewCount;

    private Double avgRating;

    /** 네이버 쇼핑 API의 productId (AI 서버 연동 시 식별자로 사용) */
    @Column(name = "naver_product_id")
    private String naverProductId;

    /**
     * Data 서버의 수집기 이름 (naver, kurly, elevenst ...). 소문자.
     *
     * <p>위의 {@link #platform} 과 다르다. platform 은 Spring 이 화면에 쓰는 표기("NAVER")이고
     * 이쪽은 Data 서버 주소의 일부다. 둘을 한 칼럼으로 합치면 표기를 바꿀 때마다
     * 외부 서버 호출이 깨진다.
     *
     * <p>{@link #naverProductId} 를 대체한다. 그쪽은 네이버만 가정한 칼럼이라
     * 쇼핑몰이 9곳으로 늘어난 지금은 상품을 특정하지 못한다.
     */
    @Column(name = "data_platform", length = 30)
    private String dataPlatform;

    /** Data 서버의 상품 ID. 쇼핑몰이 발급한 원본 값 */
    @Column(name = "data_product_id", length = 100)
    private String dataProductId;

    /** 소분류 (Naver category3 값, 예: "스마트폰", "이어폰") */
    @Column(name = "sub_category", length = 100)
    private String subCategory;

    private LocalDateTime createdAt;

    /** 멀티 플랫폼 구매 링크 (NAVER, COUPANG, 11ST 등) */
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "product_platform_links",
                     joinColumns = @JoinColumn(name = "product_id"))
    @Builder.Default
    private List<PlatformLink> platformLinks = new ArrayList<>();

    /**
     * Data 서버에서 이 상품을 가리키는 주소가 있는지.
     *
     * <p>없으면 Spring 안에서만 존재하는 상품이다(더미, 또는 Data 서버 연동 전에 만든 행).
     */
    public boolean hasDataServerAddress() {
        return dataPlatform != null && !dataPlatform.isBlank()
                && dataProductId != null && !dataProductId.isBlank();
    }

    /**
     * Data 서버 주소를 {@code "{platform}-{productId}"} 한 문자열로.
     * 챗봇 세션이나 알림 링크처럼 식별자를 한 칸에 담아야 하는 자리에서 쓴다.
     *
     * @return 주소가 없으면 null
     */
    public String dataServerExternalId() {
        return hasDataServerAddress() ? dataPlatform + "-" + dataProductId : null;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (platform == null) platform = "NAVER";
        // avgRti 는 채우지 않는다. 예전에는 NOT NULL 이라 50.0 을 넣었는데, 그러면 분석도
        // 하지 않은 상품에 신뢰도 50점이 붙어 화면이 그 수치를 실제 분석 결과처럼 보여준다.
        // 모르는 값은 null 로 둔다.
        if (reviewCount == null) reviewCount = 0;
        if (avgRating == null) avgRating = 0.0;
    }

    /** AI 서버 분석 결과로 평균 RTI 업데이트 */
    public void updateAvgRti(double newAvgRti) {
        this.avgRti = newAvgRti;
    }

    /** AI 서버 분석 결과로 리뷰 수 업데이트 */
    public void updateReviewCount(int count) {
        this.reviewCount = count;
    }

    /** 최저가 반환 (platformLinks에서 가장 낮은 가격, 없으면 기본 price) */
    public Long getLowestPrice() {
        return platformLinks.stream()
                .filter(l -> l.getPrice() != null)
                .min(Comparator.comparingLong(PlatformLink::getPrice))
                .map(PlatformLink::getPrice)
                .orElse(price);
    }

    /** 최저가 플랫폼 이름 반환 */
    public String getLowestPlatform() {
        return platformLinks.stream()
                .filter(l -> l.getPrice() != null)
                .min(Comparator.comparingLong(PlatformLink::getPrice))
                .map(PlatformLink::getPlatform)
                .orElse(platform);
    }
}
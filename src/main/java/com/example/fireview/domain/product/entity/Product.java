package com.example.fireview.domain.product.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String imageUrl;

    private Long price;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Category category;

    @Column(nullable = false)
    private String platform;

    @Column(nullable = false)
    private Double avgRti;

    private Integer reviewCount;

    private Double avgRating;

    /** 네이버 쇼핑 API의 productId (AI 서버 연동 시 식별자로 사용) */
    @Column(name = "naver_product_id")
    private String naverProductId;

    private LocalDateTime createdAt;

    /** 멀티 플랫폼 구매 링크 (NAVER, COUPANG, 11ST 등) */
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "product_platform_links",
                     joinColumns = @JoinColumn(name = "product_id"))
    @Builder.Default
    private List<PlatformLink> platformLinks = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (platform == null) platform = "NAVER";
        if (avgRti == null) avgRti = 50.0;
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
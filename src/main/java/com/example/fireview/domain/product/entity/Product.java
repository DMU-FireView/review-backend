package com.example.fireview.domain.product.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

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

    private LocalDateTime createdAt;

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
}
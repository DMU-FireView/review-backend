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
}
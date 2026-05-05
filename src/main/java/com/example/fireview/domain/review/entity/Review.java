package com.example.fireview.domain.review.entity;

import com.example.fireview.domain.product.entity.Product;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "reviews")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private String reviewerNickname;

    @Column(nullable = false)
    private String reviewerId;

    @Column(length = 2000)
    private String content;

    private Integer rating;

    @Column(nullable = false)
    private Double rtiScore;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TrustGrade trustGrade;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "review_reasons", joinColumns = @JoinColumn(name = "review_id"))
    @Column(name = "reason", length = 500)
    @Builder.Default
    private List<String> reasons = new ArrayList<>();

    @Column(nullable = false)
    private LocalDateTime writtenAt;

    private Boolean isVerifiedPurchase;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (isVerifiedPurchase == null) isVerifiedPurchase = false;
    }
}
package com.example.fireview.domain.product.dto;

import com.example.fireview.domain.product.entity.Category;
import com.example.fireview.domain.product.entity.Product;
import com.example.fireview.domain.review.entity.TrustGrade;

public record ProductResponse(
        Long id,
        String name,
        String imageUrl,
        Long price,
        Category category,
        String categoryDisplayName,
        String platform,
        Double avgRti,
        TrustGrade rtiGrade,
        String rtiColor,
        Integer reviewCount,
        Double avgRating
) {
    public static ProductResponse from(Product product) {
        TrustGrade grade = TrustGrade.fromScore(product.getAvgRti());
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getImageUrl(),
                product.getPrice(),
                product.getCategory(),
                product.getCategory().getDisplayName(),
                product.getPlatform(),
                product.getAvgRti(),
                grade,
                grade.getColor(),
                product.getReviewCount(),
                product.getAvgRating()
        );
    }
}
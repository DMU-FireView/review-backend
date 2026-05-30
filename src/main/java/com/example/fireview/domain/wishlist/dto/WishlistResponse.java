package com.example.fireview.domain.wishlist.dto;

import com.example.fireview.domain.product.dto.ProductResponse;
import com.example.fireview.domain.wishlist.entity.Wishlist;

import java.time.LocalDateTime;

public record WishlistResponse(
        Long wishlistId,
        Long productId,
        String name,
        String imageUrl,
        Long price,
        String categoryDisplayName,
        String platform,
        Double avgRti,
        String rtiGrade,
        String rtiColor,
        Integer reviewCount,
        Double avgRating,
        LocalDateTime addedAt
) {
    public static WishlistResponse from(Wishlist wishlist) {
        var product = wishlist.getProduct();
        var p = ProductResponse.from(product);
        return new WishlistResponse(
                wishlist.getId(),
                product.getId(),
                product.getName(),
                product.getImageUrl(),
                product.getPrice(),
                product.getCategory().getDisplayName(),
                product.getPlatform(),
                p.avgRti(),
                p.rtiGrade().name(),
                p.rtiColor(),
                product.getReviewCount(),
                product.getAvgRating(),
                wishlist.getCreatedAt()
        );
    }
}

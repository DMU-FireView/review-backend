package com.example.fireview.domain.cart.dto;

import com.example.fireview.domain.cart.entity.CartItem;
import com.example.fireview.domain.product.dto.ProductResponse;

import java.time.LocalDateTime;

public record CartItemResponse(
        Long cartItemId,
        Long productId,
        String name,
        String imageUrl,
        Long price,
        Long totalPrice,       // price * quantity
        String categoryDisplayName,
        String platform,
        Double avgRti,
        String rtiGrade,
        String rtiColor,
        Integer reviewCount,
        Double avgRating,
        int quantity,
        LocalDateTime addedAt
) {
    public static CartItemResponse from(CartItem item) {
        var product = item.getProduct();
        var p = ProductResponse.from(product);
        return new CartItemResponse(
                item.getId(),
                product.getId(),
                product.getName(),
                product.getImageUrl(),
                product.getPrice(),
                product.getPrice() == null ? 0L : product.getPrice() * item.getQuantity(),
                product.getCategory().getDisplayName(),
                product.getPlatform(),
                p.avgRti(),
                p.rtiGrade().name(),
                p.rtiColor(),
                product.getReviewCount(),
                product.getAvgRating(),
                item.getQuantity(),
                item.getCreatedAt()
        );
    }
}

package com.example.fireview.domain.cart.dto;

import java.util.List;

public record CartSummaryResponse(
        List<CartItemResponse> items,
        int totalCount,
        long subtotal,      // 상품 금액 합계
        long shippingFee,   // 배송비 (현재 0원 무료)
        long totalPrice     // 최종 결제 금액
) {
    public static CartSummaryResponse of(List<CartItemResponse> items) {
        long subtotal = items.stream()
                .mapToLong(CartItemResponse::totalPrice)
                .sum();
        return new CartSummaryResponse(
                items,
                items.size(),
                subtotal,
                0L,      // 무료 배송
                subtotal
        );
    }
}

package com.example.fireview.domain.cart.controller;

import com.example.fireview.domain.cart.dto.CartItemResponse;
import com.example.fireview.domain.cart.dto.CartSummaryResponse;
import com.example.fireview.domain.cart.service.CartService;
import com.example.fireview.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    /**
     * 장바구니 조회
     * GET /api/cart
     */
    @GetMapping
    public ApiResponse<CartSummaryResponse> getCart(
            @AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.success(cartService.getCart(jwt.getSubject()));
    }

    /**
     * 장바구니 상품 추가
     * POST /api/cart/{productId}?quantity=1
     */
    @PostMapping("/{productId}")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CartItemResponse> addToCart(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "1") int quantity,
            @AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.success("장바구니에 추가됐습니다.",
                cartService.addToCart(jwt.getSubject(), productId, quantity));
    }

    /**
     * 수량 변경
     * PUT /api/cart/{productId}?quantity=3
     */
    @PutMapping("/{productId}")
    public ApiResponse<CartItemResponse> updateQuantity(
            @PathVariable Long productId,
            @RequestParam int quantity,
            @AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.success(
                cartService.updateQuantity(jwt.getSubject(), productId, quantity));
    }

    /**
     * 장바구니 상품 삭제
     * DELETE /api/cart/{productId}
     */
    @DeleteMapping("/{productId}")
    public ApiResponse<Void> removeFromCart(
            @PathVariable Long productId,
            @AuthenticationPrincipal Jwt jwt) {
        cartService.removeFromCart(jwt.getSubject(), productId);
        return ApiResponse.ok("장바구니에서 삭제됐습니다.");
    }

    /**
     * 장바구니 전체 비우기
     * DELETE /api/cart
     */
    @DeleteMapping
    public ApiResponse<Void> clearCart(
            @AuthenticationPrincipal Jwt jwt) {
        cartService.clearCart(jwt.getSubject());
        return ApiResponse.ok("장바구니를 비웠습니다.");
    }
}

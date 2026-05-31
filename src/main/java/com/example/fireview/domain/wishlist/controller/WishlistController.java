package com.example.fireview.domain.wishlist.controller;

import com.example.fireview.domain.wishlist.dto.WishlistResponse;
import com.example.fireview.domain.wishlist.service.WishlistService;
import com.example.fireview.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/wishlist")
@RequiredArgsConstructor
public class WishlistController {

    private final WishlistService wishlistService;

    /**
     * 찜 목록 조회
     * GET /api/wishlist
     */
    @GetMapping
    public ApiResponse<List<WishlistResponse>> getWishlist(
            @AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.success(wishlistService.getWishlist(jwt.getSubject()));
    }

    /**
     * 찜 추가
     * POST /api/wishlist/{productId}
     */
    @PostMapping("/{productId}")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<WishlistResponse> addWishlist(
            @PathVariable Long productId,
            @AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.success("찜 목록에 추가됐습니다.",
                wishlistService.addWishlist(jwt.getSubject(), productId));
    }

    /**
     * 찜 삭제
     * DELETE /api/wishlist/{productId}
     */
    @DeleteMapping("/{productId}")
    public ApiResponse<Void> removeWishlist(
            @PathVariable Long productId,
            @AuthenticationPrincipal Jwt jwt) {
        wishlistService.removeWishlist(jwt.getSubject(), productId);
        return ApiResponse.ok("찜 목록에서 삭제됐습니다.");
    }

    /**
     * 찜 여부 확인
     * GET /api/wishlist/{productId}/check
     */
    @GetMapping("/{productId}/check")
    public ApiResponse<Map<String, Boolean>> checkWishlist(
            @PathVariable Long productId,
            @AuthenticationPrincipal Jwt jwt) {
        boolean isWishlisted = wishlistService.isWishlisted(jwt.getSubject(), productId);
        return ApiResponse.success(Map.of("wishlisted", isWishlisted));
    }
}

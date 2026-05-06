package com.example.fireview.domain.product.controller;

import com.example.fireview.domain.dashboard.service.DashboardService;
import com.example.fireview.domain.product.cache.NaverProductCache;
import com.example.fireview.domain.product.dto.ProductResponse;
import com.example.fireview.domain.product.service.ProductService;
import com.example.fireview.domain.review.dto.ReviewResponse;
import com.example.fireview.domain.review.service.ReviewService;
import com.example.fireview.domain.search.service.NaverSearchService;
import com.example.fireview.global.exception.CustomException;
import com.example.fireview.global.exception.ErrorCode;
import com.example.fireview.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final ReviewService reviewService;
    private final DashboardService dashboardService;
    private final NaverSearchService naverSearchService;
    private final NaverProductCache naverProductCache;

    @GetMapping
    public ApiResponse<List<ProductResponse>> getProducts(
            @RequestParam(required = false) String keyword) {
        if (keyword != null && !keyword.isBlank()) {
            // 네이버 API + DB 병합 결과 반환 (DB만 조회 시 결과 부족 문제 해결)
            return ApiResponse.success(naverSearchService.search(keyword, 100).products());
        }
        return ApiResponse.success(productService.getAllProducts());
    }

    @GetMapping("/{id}")
    public ApiResponse<ProductResponse> getProduct(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt) {
        // DB에서 조회 시도
        try {
            if (jwt != null) {
                dashboardService.recordView(jwt.getSubject(), id);
            }
            return ApiResponse.success(productService.getProduct(id));
        } catch (CustomException e) {
            if (e.getErrorCode() == ErrorCode.PRODUCT_NOT_FOUND) {
                // DB에 없으면 네이버 캐시에서 조회 후 DB에 자동 저장
                return naverProductCache.get(id)
                        .map(cached -> {
                            ProductResponse saved = productService.saveFromCache(cached);
                            if (jwt != null) {
                                dashboardService.recordView(jwt.getSubject(), saved.id());
                            }
                            return ApiResponse.success(saved);
                        })
                        .orElseThrow(() -> e);
            }
            throw e;
        }
    }

    @GetMapping("/{productId}/reviews")
    public ApiResponse<List<ReviewResponse>> getReviews(
            @PathVariable Long productId,
            @RequestParam(required = false) Double minScore) {
        return ApiResponse.success(reviewService.getReviewsByProduct(productId, minScore));
    }
}
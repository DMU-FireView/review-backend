package com.example.fireview.domain.product.controller;

import com.example.fireview.domain.dashboard.service.DashboardService;
import com.example.fireview.domain.product.dto.ProductResponse;
import com.example.fireview.domain.product.service.ProductService;
import com.example.fireview.domain.review.dto.ReviewResponse;
import com.example.fireview.domain.review.service.ReviewService;
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

    @GetMapping
    public ApiResponse<List<ProductResponse>> getProducts(
            @RequestParam(required = false) String keyword) {
        if (keyword != null && !keyword.isBlank()) {
            return ApiResponse.success(productService.searchProducts(keyword));
        }
        return ApiResponse.success(productService.getAllProducts());
    }

    @GetMapping("/{id}")
    public ApiResponse<ProductResponse> getProduct(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt) {
        if (jwt != null) {
            dashboardService.recordView(jwt.getSubject(), id);
        }
        return ApiResponse.success(productService.getProduct(id));
    }

    @GetMapping("/{productId}/reviews")
    public ApiResponse<List<ReviewResponse>> getReviews(
            @PathVariable Long productId,
            @RequestParam(required = false) Double minScore) {
        return ApiResponse.success(reviewService.getReviewsByProduct(productId, minScore));
    }
}
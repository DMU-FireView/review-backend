package com.example.fireview.domain.landing.controller;

import com.example.fireview.domain.product.repository.ProductRepository;
import com.example.fireview.domain.review.repository.ReviewRepository;
import com.example.fireview.domain.user.repository.UserRepository;
import com.example.fireview.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/landing")
@RequiredArgsConstructor
public class LandingController {

    private final ProductRepository productRepository;
    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;

    @GetMapping("/stats")
    public ApiResponse<Map<String, Object>> getStats() {
        long totalReviews = reviewRepository.count();
        long totalProducts = productRepository.count();
        long totalUsers = userRepository.count();

        return ApiResponse.success(Map.of(
                "totalReviewsAnalyzed", totalReviews,
                "totalProducts", totalProducts,
                "totalUsers", totalUsers,
                "detectionAccuracy", "45% 향상",
                "serviceName", "Re:view",
                "tagline", "AI 기반 리뷰 신뢰도 분석 시스템"
        ));
    }
}
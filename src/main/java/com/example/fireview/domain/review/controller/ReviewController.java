package com.example.fireview.domain.review.controller;

import com.example.fireview.domain.review.dto.ReviewFeedbackRequest;
import com.example.fireview.domain.review.service.ReviewService;
import com.example.fireview.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping("/{reviewId}/feedback")
    public ApiResponse<Void> submitFeedback(
            @PathVariable Long reviewId,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ReviewFeedbackRequest request) {
        reviewService.submitFeedback(reviewId, jwt.getSubject(), request);
        return ApiResponse.ok("피드백이 등록되었습니다.");
    }
}
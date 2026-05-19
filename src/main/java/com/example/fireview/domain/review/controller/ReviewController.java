package com.example.fireview.domain.review.controller;

import com.example.fireview.domain.review.dto.FeedbackHistoryResponse;
import com.example.fireview.domain.review.dto.ReviewFeedbackRequest;
import com.example.fireview.domain.review.service.ReviewService;
import com.example.fireview.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    /**
     * 리뷰 피드백 제출
     * POST /api/reviews/{reviewId}/feedback
     */
    @PostMapping("/{reviewId}/feedback")
    public ApiResponse<Void> submitFeedback(
            @PathVariable Long reviewId,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ReviewFeedbackRequest request) {
        reviewService.submitFeedback(reviewId, jwt.getSubject(), request);
        return ApiResponse.ok("피드백이 등록되었습니다.");
    }

    /**
     * 내가 제출한 피드백 목록 조회
     * GET /api/reviews/feedbacks/me?page=0&size=10
     */
    @GetMapping("/feedbacks/me")
    public ApiResponse<Page<FeedbackHistoryResponse>> getMyFeedbacks(
            @AuthenticationPrincipal Jwt jwt,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return ApiResponse.success(reviewService.getMyFeedbacks(jwt.getSubject(), pageable));
    }

    /**
     * 내가 제출한 피드백 단건 조회
     * GET /api/reviews/feedbacks/me/{feedbackId}
     */
    @GetMapping("/feedbacks/me/{feedbackId}")
    public ApiResponse<FeedbackHistoryResponse> getMyFeedback(
            @PathVariable Long feedbackId,
            @AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.success(reviewService.getMyFeedback(feedbackId, jwt.getSubject()));
    }
}
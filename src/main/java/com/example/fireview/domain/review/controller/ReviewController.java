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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

@Tag(name = "리뷰 피드백", description = "리뷰 진위 피드백")
@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    /**
     * 리뷰 피드백 제출
     * POST /api/reviews/{reviewId}/feedback
     */
    @Operation(summary = "리뷰 피드백 (Data 서버 리뷰)", description = """
            `/api/v2/products/**` 로 조회한 리뷰에 '실제/가짜' 피드백을 남긴다.
            리뷰 ID 는 쇼핑몰이 발급한 원본 값(`reviews.items[].reviewId`)을 그대로 넣는다.

            상품 번호표가 없으면 이 호출에서 발급된다. 수집 전 상품이면
            `409 PRODUCT_NOT_COLLECTED` 로 거절된다.

            같은 리뷰에 두 번 남기면 `409 FEEDBACK_ALREADY_EXISTS` 다.

            피드백 내역 조회(`/api/reviews/feedbacks/me`)에서 Data 서버 리뷰는
            `reviewId` 와 본문 요약이 null 로 내려온다. 본문을 저장하지 않기 때문이다.
            """)
    @PostMapping("/external/{platform}/{productId}/reviews/{externalReviewId}/feedback")
    public ApiResponse<Void> submitExternalFeedback(
            @PathVariable String platform,
            @PathVariable String productId,
            @PathVariable String externalReviewId,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ReviewFeedbackRequest request) {
        reviewService.submitExternalFeedback(platform, productId, externalReviewId,
                jwt.getSubject(), request);
        return ApiResponse.ok("피드백이 제출되었습니다.");
    }

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
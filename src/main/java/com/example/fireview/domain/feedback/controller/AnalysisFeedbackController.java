package com.example.fireview.domain.feedback.controller;

import com.example.fireview.domain.feedback.dto.request.AnalysisFeedbackCreateRequest;
import com.example.fireview.domain.feedback.dto.response.AnalysisFeedbackResponse;
import com.example.fireview.domain.feedback.service.AnalysisFeedbackService;
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

@Tag(name = "분석 피드백", description = "AI 분석 결과에 대한 피드백")
@RestController
@RequestMapping("/api/analysis-feedbacks")
@RequiredArgsConstructor
public class AnalysisFeedbackController {

    private final AnalysisFeedbackService analysisFeedbackService;

    @Operation(summary = "분석 피드백 제출 (Data 서버 리뷰)", description = """
            `/api/v2/products/**` 로 조회한 리뷰의 분석 결과에 피드백을 남긴다. 리뷰 ID 는 쇼핑몰이 발급한
            원본 값(`reviews.items[].reviewId`, 분석 결과의 `reviewId`)을 그대로 넣는다. 최대 200자.

            Spring DB 에 그 리뷰 행이 없으므로 **본문은 저장하지 않는다.** 응답의 `reviewId` 와
            `reviewContent` 는 null 이고, `externalReviewId` 와 `productExternalId` 가 대신 채워진다.

            상품 번호표가 없으면 이 호출에서 발급된다. 아직 수집 전인 상품이면 `409 PRODUCT_NOT_COLLECTED`,
            Data 서버에 닿지 못하면 `503 DATA_SERVER_UNAVAILABLE` 이다. 리뷰 ID 가 비었거나 200자를 넘으면
            `400 INVALID_INPUT`.

            **검증하지 않는 것:** 그 리뷰가 실제로 그 상품에 있는지, 그 리뷰의 분석 결과가 있는지는
            확인하지 않는다(Data 서버에 단건 검증 API 가 없다). 분석 결과가 있는 리뷰에서만 제출 버튼을 보여 줄 것.

            기존 Spring 리뷰 경로(`/reviews/{reviewId}`)와 마찬가지로 같은 리뷰에 여러 번 제출할 수 있다.
            """)
    @PostMapping("/external/{platform}/{productId}/reviews/{externalReviewId}")
    public ApiResponse<AnalysisFeedbackResponse> submitExternal(
            @PathVariable String platform,
            @PathVariable String productId,
            @PathVariable String externalReviewId,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody AnalysisFeedbackCreateRequest request) {
        return ApiResponse.success("분석 피드백이 접수되었습니다.",
                analysisFeedbackService.submitExternal(platform, productId, externalReviewId,
                        jwt.getSubject(), request));
    }

    /** POST /api/analysis-feedbacks/reviews/{reviewId} — 분석 피드백 제출 (Spring 리뷰) */
    @PostMapping("/reviews/{reviewId}")
    public ApiResponse<AnalysisFeedbackResponse> submit(
            @PathVariable Long reviewId,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody AnalysisFeedbackCreateRequest request) {
        return ApiResponse.success("분석 피드백이 접수되었습니다.",
                analysisFeedbackService.submit(reviewId, jwt.getSubject(), request));
    }

    /** GET /api/analysis-feedbacks/me — 내 분석 피드백 목록 */
    @GetMapping("/me")
    public ApiResponse<Page<AnalysisFeedbackResponse>> getMyFeedbacks(
            @AuthenticationPrincipal Jwt jwt,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return ApiResponse.success(
                analysisFeedbackService.getMyFeedbacks(jwt.getSubject(), pageable));
    }

    /** GET /api/analysis-feedbacks/me/{feedbackId} — 내 분석 피드백 단건 */
    @GetMapping("/me/{feedbackId}")
    public ApiResponse<AnalysisFeedbackResponse> getMyFeedback(
            @PathVariable Long feedbackId,
            @AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.success(
                analysisFeedbackService.getMyFeedback(feedbackId, jwt.getSubject()));
    }
}

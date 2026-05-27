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
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/analysis-feedbacks")
@RequiredArgsConstructor
public class AnalysisFeedbackController {

    private final AnalysisFeedbackService analysisFeedbackService;

    /** POST /api/analysis-feedbacks/reviews/{reviewId} — 분석 피드백 제출 */
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

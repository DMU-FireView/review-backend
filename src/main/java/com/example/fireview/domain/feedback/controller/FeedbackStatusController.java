package com.example.fireview.domain.feedback.controller;

import com.example.fireview.domain.feedback.dto.response.UnifiedFeedbackResponse;
import com.example.fireview.domain.feedback.service.FeedbackStatusService;
import com.example.fireview.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/feedback")
@RequiredArgsConstructor
public class FeedbackStatusController {

    private final FeedbackStatusService feedbackStatusService;

    /**
     * GET /api/feedback/me
     * 내가 제출한 신고 + 분석 피드백 통합 목록 (최신순)
     */
    @GetMapping("/me")
    public ApiResponse<List<UnifiedFeedbackResponse>> getMyFeedbacks(
            @AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.success(
                feedbackStatusService.getUnifiedFeedbacks(jwt.getSubject()));
    }
}

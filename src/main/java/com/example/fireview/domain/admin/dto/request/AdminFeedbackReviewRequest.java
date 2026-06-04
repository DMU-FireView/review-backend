package com.example.fireview.domain.admin.dto.request;

import com.example.fireview.domain.feedback.entity.AnalysisFeedbackStatus;
import jakarta.validation.constraints.NotNull;

public record AdminFeedbackReviewRequest(
        @NotNull AnalysisFeedbackStatus status,
        String adminComment
) {}

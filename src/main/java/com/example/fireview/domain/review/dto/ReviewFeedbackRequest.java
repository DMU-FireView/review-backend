package com.example.fireview.domain.review.dto;

import com.example.fireview.domain.review.entity.FeedbackType;
import jakarta.validation.constraints.NotNull;

public record ReviewFeedbackRequest(
        @NotNull(message = "피드백 유형을 선택해주세요.")
        FeedbackType feedbackType
) {}
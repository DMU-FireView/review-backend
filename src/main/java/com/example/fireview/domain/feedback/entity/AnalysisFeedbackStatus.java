package com.example.fireview.domain.feedback.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AnalysisFeedbackStatus {
    SUBMITTED("접수"),
    UNDER_REVIEW("검토 중"),
    RESOLVED("처리 완료"),
    REJECTED("반려");

    private final String description;
}

package com.example.fireview.domain.feedback.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AnalysisFeedbackType {
    SCORE_MISMATCH("점수가 맞지 않아요"),
    EXPLANATION_INSUFFICIENT("설명이 부족해요"),
    INFO_INCORRECT("정보가 달라요"),
    IMPROVEMENT_SUGGESTION("개선 제안");

    private final String description;
}

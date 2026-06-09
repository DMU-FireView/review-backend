package com.example.fireview.domain.report.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 리뷰 신고 사유
 */
@Getter
@RequiredArgsConstructor
public enum ReportReason {

    FAKE_REVIEW("가짜 리뷰 / 리뷰 날바 의심"),
    AI_GENERATED("AI 생성 문체 의심"),
    IRRELEVANT_CONTENT("상품과 무관한 내용"),
    INAPPROPRIATE("부적절한 표현 / 개인정보"),
    AD_REVIEW("광고성 리뷰"),
    REPETITIVE_CONTENT("반복 내용"),
    OFFENSIVE_CONTENT("욕설 / 혐오 표현"),
    OTHER("기타");

    private final String description;
}

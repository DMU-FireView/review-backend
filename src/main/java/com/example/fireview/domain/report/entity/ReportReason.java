package com.example.fireview.domain.report.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 리뷰 신고 사유
 */
@Getter
@RequiredArgsConstructor
public enum ReportReason {

    FAKE_REVIEW("가짜 리뷰 의심"),
    AD_REVIEW("광고성 리뷰"),
    REPETITIVE_CONTENT("반복 내용"),
    IRRELEVANT_CONTENT("상품과 무관한 내용"),
    OFFENSIVE_CONTENT("욕설 / 혐오 표현"),
    PERSONAL_INFO("개인정보 포함"),
    OTHER("기타");

    private final String description;
}

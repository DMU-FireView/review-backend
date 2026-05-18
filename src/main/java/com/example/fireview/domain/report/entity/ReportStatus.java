package com.example.fireview.domain.report.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 신고 처리 상태
 */
@Getter
@RequiredArgsConstructor
public enum ReportStatus {

    PENDING("접수"),
    UNDER_REVIEW("검토중"),
    ACCEPTED("처리완료 - 신고 인정"),
    REJECTED("처리완료 - 신고 기각");

    private final String description;
}

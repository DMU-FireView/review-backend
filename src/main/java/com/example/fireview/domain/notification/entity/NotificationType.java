package com.example.fireview.domain.notification.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 알림 유형
 */
@Getter
@RequiredArgsConstructor
public enum NotificationType {

    REPORT_RECEIVED("신고 접수"),
    REPORT_UNDER_REVIEW("신고 검토 시작"),
    REPORT_ACCEPTED("신고 처리완료 - 인정"),
    REPORT_REJECTED("신고 처리완료 - 기각"),
    SYSTEM("시스템 알림");

    private final String description;
}

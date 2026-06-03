package com.example.fireview.domain.notification.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 알림 유형.
 * settingKey 는 UserSetting 의 필드명과 대응된다.
 * null 이면 설정과 무관하게 항상 발송한다(SYSTEM 알림 등).
 */
@Getter
@RequiredArgsConstructor
public enum NotificationType {

    /** 신고 관련 — notifyFeedbackResult 설정 참조 */
    REPORT_RECEIVED("신고 접수", "notifyFeedbackResult"),
    REPORT_UNDER_REVIEW("신고 검토 시작", "notifyFeedbackResult"),
    REPORT_ACCEPTED("신고 처리완료 - 인정", "notifyFeedbackResult"),
    REPORT_REJECTED("신고 처리완료 - 기각", "notifyFeedbackResult"),

    /** 분석 피드백 관련 — notifyFeedbackResult 설정 참조 */
    ANALYSIS_FEEDBACK_RECEIVED("분석 피드백 접수", "notifyFeedbackResult"),
    ANALYSIS_FEEDBACK_UNDER_REVIEW("분석 피드백 검토 시작", "notifyFeedbackResult"),
    ANALYSIS_FEEDBACK_RESOLVED("분석 피드백 처리완료 - 반영", "notifyFeedbackResult"),
    ANALYSIS_FEEDBACK_REJECTED("분석 피드백 처리완료 - 기각", "notifyFeedbackResult"),

    /** AI 분석 완료 — notifyAnalysisComplete 설정 참조 */
    ANALYSIS_COMPLETE("AI 분석 완료", "notifyAnalysisComplete"),

    /** 위험 상품 감지 — notifyRiskyProduct 설정 참조 */
    RISKY_PRODUCT_DETECTED("위험 상품 감지", "notifyRiskyProduct"),

    /** 시스템 공지 — 설정과 무관하게 항상 발송 */
    SYSTEM("시스템 알림", null);

    private final String description;

    /** UserSetting 필드명. null 이면 항상 발송 */
    private final String settingKey;
}

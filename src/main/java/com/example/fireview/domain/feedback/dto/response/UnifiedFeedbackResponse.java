package com.example.fireview.domain.feedback.dto.response;

import com.example.fireview.domain.feedback.entity.AnalysisFeedback;
import com.example.fireview.domain.report.entity.Report;

import java.time.LocalDateTime;

/**
 * 신고 + 분석 피드백 통합 조회 응답
 *
 * 피드백 내역 화면에서 유형에 관계없이 한 목록으로 표시하기 위한 DTO.
 * Data 서버 리뷰를 대상으로 한 신고·분석 피드백은 {@code reviewContent} 가 null 이다.
 */
public record UnifiedFeedbackResponse(
        Long id,
        String feedbackCategory,     // REPORT / ANALYSIS_FEEDBACK
        String typeLabel,
        String productName,
        String reviewContent,
        String status,
        String statusDescription,
        int currentStep,
        int totalSteps,
        LocalDateTime createdAt
) {
    public static UnifiedFeedbackResponse fromReport(Report r) {
        int step = switch (r.getStatus()) {
            case PENDING      -> 1;
            case UNDER_REVIEW -> 2;
            case ACCEPTED, REJECTED -> 4;
        };
        return new UnifiedFeedbackResponse(
                r.getId(),
                "REPORT",
                "리뷰 신고",
                r.productNameOrNull(),
                r.reviewContentOrNull(),
                r.getStatus().name(),
                r.getStatus().getDescription(),
                step, 4,
                r.getCreatedAt()
        );
    }

    public static UnifiedFeedbackResponse fromAnalysisFeedback(AnalysisFeedback f) {
        int step = switch (f.getStatus()) {
            case SUBMITTED    -> 1;
            case UNDER_REVIEW -> 2;
            case RESOLVED, REJECTED -> 4;
        };
        return new UnifiedFeedbackResponse(
                f.getId(),
                "ANALYSIS_FEEDBACK",
                f.getFeedbackType().getDescription(),
                f.productNameOrNull(),
                f.reviewContentOrNull(),
                f.getStatus().name(),
                f.getStatus().getDescription(),
                step, 4,
                f.getCreatedAt()
        );
    }
}

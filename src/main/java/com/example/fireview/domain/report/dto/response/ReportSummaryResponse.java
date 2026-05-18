package com.example.fireview.domain.report.dto.response;

import com.example.fireview.domain.report.entity.Report;
import com.example.fireview.domain.report.entity.ReportReason;
import com.example.fireview.domain.report.entity.ReportStatus;

import java.time.LocalDateTime;

/**
 * 신고 목록용 요약 응답 DTO (content 전체 대신 요약만)
 */
public record ReportSummaryResponse(
        Long reportId,
        Long reviewId,
        String reviewContentSummary,
        String productName,
        ReportReason reason,
        String reasonDescription,
        ReportStatus status,
        String statusDescription,
        LocalDateTime createdAt
) {
    public static ReportSummaryResponse from(Report report) {
        String content = report.getReview().getContent();
        String summary = (content != null && content.length() > 50)
                ? content.substring(0, 50) + "..."
                : content;

        return new ReportSummaryResponse(
                report.getId(),
                report.getReview().getId(),
                summary,
                report.getReview().getProduct().getName(),
                report.getReason(),
                report.getReason().getDescription(),
                report.getStatus(),
                report.getStatus().getDescription(),
                report.getCreatedAt()
        );
    }
}

package com.example.fireview.domain.report.dto.response;

import com.example.fireview.domain.report.entity.Report;
import com.example.fireview.domain.report.entity.ReportReason;
import com.example.fireview.domain.report.entity.ReportStatus;

import java.time.LocalDateTime;

public record ReportResponse(
        Long reportId,
        Long reviewId,
        String reviewContent,
        String productName,
        ReportReason reason,
        String reasonDescription,
        String detail,
        String attachmentUrl,
        boolean includeAiEvidence,
        ReportStatus status,
        String statusDescription,
        String adminComment,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static ReportResponse from(Report report) {
        return new ReportResponse(
                report.getId(),
                report.getReview().getId(),
                report.getReview().getContent(),
                report.getReview().getProduct().getName(),
                report.getReason(),
                report.getReason().getDescription(),
                report.getDetail(),
                report.getAttachmentUrl(),
                report.isIncludeAiEvidence(),
                report.getStatus(),
                report.getStatus().getDescription(),
                report.getAdminComment(),
                report.getCreatedAt(),
                report.getUpdatedAt()
        );
    }
}

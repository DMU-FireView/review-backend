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
        LocalDateTime updatedAt,

        // Data 서버 리뷰 신고일 때만 채워진다
        String externalReviewId,
        String productExternalId
) {
    /**
     * <p>Data 서버 리뷰 신고는 {@code reviewId} 와 {@code reviewContent} 가 null 이다.
     * Spring DB 에 그 리뷰 행이 없고 본문도 복사해 두지 않는다.
     * 대신 {@code externalReviewId} 와 {@code productExternalId} 로 찾아간다.
     */
    public static ReportResponse from(Report report) {
        return new ReportResponse(
                report.getId(),
                report.reviewIdOrNull(),
                report.reviewContentOrNull(),
                report.productNameOrNull(),
                report.getReason(),
                report.getReason().getDescription(),
                report.getDetail(),
                report.getAttachmentUrl(),
                report.isIncludeAiEvidence(),
                report.getStatus(),
                report.getStatus().getDescription(),
                report.getAdminComment(),
                report.getCreatedAt(),
                report.getUpdatedAt(),
                report.getExternalReviewId(),
                report.productExternalIdOrNull()
        );
    }
}

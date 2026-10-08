package com.example.fireview.domain.feedback.dto.response;

import com.example.fireview.domain.feedback.entity.AnalysisFeedback;
import com.example.fireview.domain.feedback.entity.AnalysisFeedbackStatus;
import com.example.fireview.domain.feedback.entity.AnalysisFeedbackType;
import com.example.fireview.domain.feedback.entity.UserJudgment;

import java.time.LocalDateTime;
import java.util.List;

public record AnalysisFeedbackResponse(
        Long feedbackId,
        Long reviewId,
        String reviewContent,
        String productName,
        AnalysisFeedbackType feedbackType,
        String feedbackTypeDescription,
        UserJudgment userJudgment,
        List<String> relatedSignals,
        String detail,
        String attachmentUrl,
        String replyEmail,
        AnalysisFeedbackStatus status,
        String statusDescription,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,

        // Data 서버 리뷰에 대한 피드백일 때만 채워진다
        String externalReviewId,
        String productExternalId
) {
    /**
     * <p>Data 서버 리뷰에 대한 피드백은 {@code reviewId} 와 {@code reviewContent} 가 null 이다.
     * Spring DB 에 그 리뷰 행이 없고 본문도 복사해 두지 않는다.
     * 대신 {@code externalReviewId} 와 {@code productExternalId} 로 찾아간다.
     */
    public static AnalysisFeedbackResponse from(AnalysisFeedback f) {
        return new AnalysisFeedbackResponse(
                f.getId(),
                f.reviewIdOrNull(),
                f.reviewContentOrNull(),
                f.productNameOrNull(),
                f.getFeedbackType(),
                f.getFeedbackType().getDescription(),
                f.getUserJudgment(),
                f.getRelatedSignals(),
                f.getDetail(),
                f.getAttachmentUrl(),
                f.getReplyEmail(),
                f.getStatus(),
                f.getStatus().getDescription(),
                f.getCreatedAt(),
                f.getUpdatedAt(),
                f.getExternalReviewId(),
                f.productExternalIdOrNull()
        );
    }
}

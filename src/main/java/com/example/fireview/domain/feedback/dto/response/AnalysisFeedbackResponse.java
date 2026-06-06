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
        LocalDateTime updatedAt
) {
    public static AnalysisFeedbackResponse from(AnalysisFeedback f) {
        return new AnalysisFeedbackResponse(
                f.getId(),
                f.getReview().getId(),
                f.getReview().getContent(),
                f.getReview().getProduct().getName(),
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
                f.getUpdatedAt()
        );
    }
}

package com.example.fireview.domain.review.dto;

import com.example.fireview.domain.review.entity.FeedbackType;
import com.example.fireview.domain.review.entity.ReviewFeedback;

import java.time.LocalDateTime;

/**
 * 내가 제출한 피드백 내역 응답 DTO
 */
public record FeedbackHistoryResponse(
        Long feedbackId,
        Long reviewId,
        String reviewContentSummary,
        Long productId,
        String productName,
        FeedbackType feedbackType,
        String feedbackTypeLabel,
        LocalDateTime createdAt
) {
    public static FeedbackHistoryResponse from(ReviewFeedback feedback) {
        String content = feedback.getReview().getContent();
        String summary = (content != null && content.length() > 50)
                ? content.substring(0, 50) + "..."
                : content;

        return new FeedbackHistoryResponse(
                feedback.getId(),
                feedback.getReview().getId(),
                summary,
                feedback.getReview().getProduct().getId(),
                feedback.getReview().getProduct().getName(),
                feedback.getFeedbackType(),
                feedback.getFeedbackType() == FeedbackType.REAL ? "실제 리뷰" : "가짜 리뷰",
                feedback.getCreatedAt()
        );
    }
}

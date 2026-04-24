package com.example.fireview.domain.review.dto;

import com.example.fireview.domain.review.entity.Review;
import com.example.fireview.domain.review.entity.TrustGrade;

import java.time.LocalDateTime;
import java.util.List;

public record ReviewResponse(
        Long id,
        Long productId,
        String reviewerNickname,
        String content,
        Integer rating,
        Double rtiScore,
        TrustGrade trustGrade,
        String trustGradeLabel,
        String trustGradeColor,
        List<String> reasons,
        LocalDateTime writtenAt,
        Boolean isVerifiedPurchase
) {
    public static ReviewResponse from(Review review) {
        return new ReviewResponse(
                review.getId(),
                review.getProduct().getId(),
                review.getReviewerNickname(),
                review.getContent(),
                review.getRating(),
                review.getRtiScore(),
                review.getTrustGrade(),
                review.getTrustGrade().getLabel(),
                review.getTrustGrade().getColor(),
                review.getReasons(),
                review.getWrittenAt(),
                review.getIsVerifiedPurchase()
        );
    }
}
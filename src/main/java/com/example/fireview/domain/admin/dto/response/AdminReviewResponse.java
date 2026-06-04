package com.example.fireview.domain.admin.dto.response;

import com.example.fireview.domain.review.entity.Review;
import com.example.fireview.domain.review.entity.TrustGrade;

import java.time.LocalDateTime;
import java.util.List;

public record AdminReviewResponse(
        Long reviewId,
        String productId,
        String productName,
        String reviewerNickname,
        String content,
        Integer rating,
        double rtiScore,
        TrustGrade trustGrade,
        List<String> reasons,
        Boolean isVerifiedPurchase,
        LocalDateTime writtenAt
) {
    public static AdminReviewResponse from(Review r) {
        return new AdminReviewResponse(
                r.getId(),
                r.getProduct().getId(),
                r.getProduct().getName(),
                r.getReviewerNickname(),
                r.getContent(),
                r.getRating(),
                r.getRtiScore(),
                r.getTrustGrade(),
                r.getReasons(),
                r.getIsVerifiedPurchase(),
                r.getWrittenAt()
        );
    }
}

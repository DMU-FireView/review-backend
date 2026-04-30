package com.example.fireview.domain.review.dto;

import com.example.fireview.domain.review.entity.Review;
import com.example.fireview.domain.review.entity.TrustGrade;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 개별 리뷰 응답 DTO
 *
 * [표시 정책]
 * - 개별 리뷰에는 RTI 수치를 노출하지 않습니다.
 *   대신 등급(trustGrade)과 레이블(위험/불안/안전)만 표시합니다.
 * - RTI 수치는 상품 단위(ProductResponse.avgRti)에서만 노출합니다.
 * - reviewerAtiScore: 리뷰 작성자 계정의 신뢰도 점수 (ATI, nullable)
 */
public record ReviewResponse(
        Long id,
        Long productId,
        String reviewerNickname,
        String content,
        Integer rating,
        // rtiScore 제거 - 개별 리뷰에는 RTI 수치 미노출 (이슈 #9)
        TrustGrade trustGrade,
        String trustGradeLabel,
        String trustGradeColor,
        List<String> reasons,
        LocalDateTime writtenAt,
        Boolean isVerifiedPurchase,
        Double reviewerAtiScore    // 작성자 ATI 점수 (이슈 #10, nullable)
) {
    /** 기본 팩토리 (ATI 점수 없음) */
    public static ReviewResponse from(Review review) {
        return of(review, null);
    }

    /** ATI 점수 포함 팩토리 */
    public static ReviewResponse of(Review review, Double atiScore) {
        return new ReviewResponse(
                review.getId(),
                review.getProduct().getId(),
                review.getReviewerNickname(),
                review.getContent(),
                review.getRating(),
                review.getTrustGrade(),
                review.getTrustGrade().getLabel(),
                review.getTrustGrade().getColor(),
                review.getReasons(),
                review.getWrittenAt(),
                review.getIsVerifiedPurchase(),
                atiScore
        );
    }
}
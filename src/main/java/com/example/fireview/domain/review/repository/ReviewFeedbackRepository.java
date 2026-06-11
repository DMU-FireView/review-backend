package com.example.fireview.domain.review.repository;

import com.example.fireview.domain.review.entity.ReviewFeedback;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ReviewFeedbackRepository extends JpaRepository<ReviewFeedback, Long> {

    Optional<ReviewFeedback> findByReview_IdAndUser_Id(Long reviewId, Long userId);

    boolean existsByReview_IdAndUser_Id(Long reviewId, Long userId);

    /** 내가 제출한 피드백 목록 (리뷰+상품 JOIN FETCH, 최신순) */
    @Query("SELECT f FROM ReviewFeedback f "
         + "JOIN FETCH f.review r "
         + "JOIN FETCH r.product "
         + "WHERE f.user.id = :userId "
         + "ORDER BY f.createdAt DESC")
    Page<ReviewFeedback> findByUserIdWithReview(@Param("userId") Long userId, Pageable pageable);

    /** 내가 제출한 피드백 단건 조회 (본인 확인) */
    Optional<ReviewFeedback> findByIdAndUser_Id(Long feedbackId, Long userId);

    /** 내가 제출한 피드백 수 */
    long countByUser_Id(Long userId);

    // ── 모델 성능 모니터링용 ───────────────────────────────────────────────────

    /**
     * AI 판정과 사용자 피드백이 일치하는 건수 (동의율 계산용)
     * - AI SAFE 리뷰 → 사용자 REAL 피드백 : 동의
     * - AI SUSPICIOUS/DANGER 리뷰 → 사용자 FAKE 피드백 : 동의
     */
    @Query("SELECT COUNT(f) FROM ReviewFeedback f " +
           "WHERE (f.review.trustGrade = com.example.fireview.domain.review.entity.TrustGrade.SAFE " +
           "       AND f.feedbackType = com.example.fireview.domain.review.entity.FeedbackType.REAL) " +
           "   OR (f.review.trustGrade <> com.example.fireview.domain.review.entity.TrustGrade.SAFE " +
           "       AND f.feedbackType = com.example.fireview.domain.review.entity.FeedbackType.FAKE)")
    long countAgreementFeedbacks();
}
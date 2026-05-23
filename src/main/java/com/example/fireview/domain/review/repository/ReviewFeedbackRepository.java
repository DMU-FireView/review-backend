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
}
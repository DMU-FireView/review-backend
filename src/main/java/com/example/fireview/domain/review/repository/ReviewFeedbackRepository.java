package com.example.fireview.domain.review.repository;

import com.example.fireview.domain.review.entity.ReviewFeedback;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReviewFeedbackRepository extends JpaRepository<ReviewFeedback, Long> {
    Optional<ReviewFeedback> findByReview_IdAndUser_Id(Long reviewId, Long userId);
    boolean existsByReview_IdAndUser_Id(Long reviewId, Long userId);
}
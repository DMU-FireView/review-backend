package com.example.fireview.domain.review.repository;

import com.example.fireview.domain.review.entity.Review;
import com.example.fireview.domain.review.entity.TrustGrade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByProduct_IdOrderByRtiScoreDesc(Long productId);
    List<Review> findByProduct_IdAndTrustGrade(Long productId, TrustGrade grade);
    List<Review> findByProduct_IdAndRtiScoreGreaterThanEqual(Long productId, double minScore);

    @Query("SELECT AVG(r.rtiScore) FROM Review r WHERE r.product.id = :productId")
    Double findAvgRtiByProductId(Long productId);
}
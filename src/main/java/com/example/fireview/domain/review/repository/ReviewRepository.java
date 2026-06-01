package com.example.fireview.domain.review.repository;

import com.example.fireview.domain.product.entity.Product;
import com.example.fireview.domain.review.entity.Review;
import com.example.fireview.domain.review.entity.TrustGrade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByProduct(Product product);
    boolean existsByProduct_IdAndReviewerIdAndWrittenAt(Long productId, String reviewerId, java.time.LocalDateTime writtenAt);
    List<Review> findByProduct_IdOrderByRtiScoreDesc(Long productId);
    List<Review> findByProduct_IdAndTrustGrade(Long productId, TrustGrade grade);
    List<Review> findByProduct_IdAndRtiScoreGreaterThanEqual(Long productId, double minScore);

    @Query("SELECT AVG(r.rtiScore) FROM Review r WHERE r.product.id = :productId")
    Double findAvgRtiByProductId(Long productId);

    /** ATI 계산: 특정 작성자의 모든 리뷰 평균 RTI */
    @Query("SELECT AVG(r.rtiScore) FROM Review r WHERE r.reviewerId = :reviewerId")
    Double findAvgRtiByReviewerId(@Param("reviewerId") String reviewerId);

    /** 상품의 리뷰 작성자별 평균 RTI 일괄 조회 (ATI 계산용, N+1 방지) */
    @Query("SELECT r.reviewerId AS reviewerId, AVG(r.rtiScore) AS avgRti " +
           "FROM Review r WHERE r.product.id = :productId GROUP BY r.reviewerId")
    List<ReviewerAtiProjection> findReviewerAtiByProductId(@Param("productId") Long productId);

    /** ATI 계산용 프로젝션 */
    interface ReviewerAtiProjection {
        String getReviewerId();
        Double getAvgRti();
    }
}
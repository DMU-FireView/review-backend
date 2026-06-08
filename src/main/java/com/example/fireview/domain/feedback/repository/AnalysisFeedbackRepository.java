package com.example.fireview.domain.feedback.repository;

import com.example.fireview.domain.feedback.entity.AnalysisFeedback;
import com.example.fireview.domain.feedback.entity.AnalysisFeedbackStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AnalysisFeedbackRepository extends JpaRepository<AnalysisFeedback, Long> {

    @Query("SELECT f FROM AnalysisFeedback f JOIN FETCH f.review r JOIN FETCH r.product "
         + "WHERE f.submitter.id = :userId ORDER BY f.createdAt DESC")
    Page<AnalysisFeedback> findBySubmitterIdWithReview(@Param("userId") Long userId, Pageable pageable);

    Page<AnalysisFeedback> findByStatus(AnalysisFeedbackStatus status, Pageable pageable);

    @Query("SELECT f FROM AnalysisFeedback f JOIN FETCH f.submitter JOIN FETCH f.review "
         + "ORDER BY f.createdAt DESC")
    Page<AnalysisFeedback> findAllWithDetails(Pageable pageable);

    // ── 모델 성능 모니터링용 ───────────────────────────────────────────────────

    /** 상태별 분석 피드백 수 집계 */
    long countByStatus(AnalysisFeedbackStatus status);
}

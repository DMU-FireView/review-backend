package com.example.fireview.domain.report.repository;

import com.example.fireview.domain.report.entity.Report;
import com.example.fireview.domain.report.entity.ReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ReportRepository extends JpaRepository<Report, Long> {

    /** 특정 사용자가 특정 리뷰를 이미 신고했는지 확인 */
    boolean existsByReporter_IdAndReview_Id(Long reporterId, Long reviewId);

    /** 내가 신고한 목록 (최신순) */
    @Query("SELECT r FROM Report r JOIN FETCH r.review rv JOIN FETCH rv.product "
         + "WHERE r.reporter.id = :userId ORDER BY r.createdAt DESC")
    Page<Report> findByReporterIdWithReview(@Param("userId") Long userId, Pageable pageable);

    /** 특정 신고 단건 조회 (신고자 본인 확인용) */
    Optional<Report> findByIdAndReporter_Id(Long reportId, Long reporterId);

    /** 전체 신고 목록 (관리자용, 상태 필터) */
    Page<Report> findByStatus(ReportStatus status, Pageable pageable);

    /** 전체 신고 목록 (관리자용, 전체) */
    @Query("SELECT r FROM Report r JOIN FETCH r.reporter JOIN FETCH r.review "
         + "ORDER BY r.createdAt DESC")
    Page<Report> findAllWithDetails(Pageable pageable);
}

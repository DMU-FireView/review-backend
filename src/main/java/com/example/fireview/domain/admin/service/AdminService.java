package com.example.fireview.domain.admin.service;

import com.example.fireview.domain.admin.dto.response.AdminDashboardResponse;
import com.example.fireview.domain.admin.dto.response.AdminModelPerformanceResponse;
import com.example.fireview.domain.admin.dto.response.AdminModelPerformanceResponse.AnalysisFeedbackStats;
import com.example.fireview.domain.admin.dto.response.AdminModelPerformanceResponse.DailyRtiTrend;
import com.example.fireview.domain.admin.dto.response.AdminModelPerformanceResponse.RtiDistribution;
import com.example.fireview.domain.admin.dto.response.AdminModelPerformanceResponse.UserAgreementStats;
import com.example.fireview.domain.admin.dto.response.AdminReviewResponse;
import com.example.fireview.domain.admin.dto.response.AdminUserResponse;
import com.example.fireview.domain.feedback.dto.response.AnalysisFeedbackResponse;
import com.example.fireview.domain.feedback.entity.AnalysisFeedback;
import com.example.fireview.domain.feedback.entity.AnalysisFeedbackStatus;
import com.example.fireview.domain.feedback.repository.AnalysisFeedbackRepository;
import com.example.fireview.domain.report.dto.response.ReportResponse;
import com.example.fireview.domain.report.entity.ReportStatus;
import com.example.fireview.domain.report.repository.ReportRepository;
import com.example.fireview.domain.report.service.ReportService;
import com.example.fireview.domain.review.entity.TrustGrade;
import com.example.fireview.domain.review.repository.ReviewFeedbackRepository;
import com.example.fireview.domain.review.repository.ReviewRepository;
import com.example.fireview.domain.review.repository.ReviewRepository.DailyRtiProjection;
import com.example.fireview.domain.user.repository.UserRepository;
import com.example.fireview.global.exception.CustomException;
import com.example.fireview.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminService {

    private final ReportRepository reportRepository;
    private final ReportService reportService;
    private final ReviewRepository reviewRepository;
    private final ReviewFeedbackRepository reviewFeedbackRepository;
    private final AnalysisFeedbackRepository analysisFeedbackRepository;
    private final UserRepository userRepository;

    // ── 대시보드 ─────────────────────────────────────────────────────────────

    public AdminDashboardResponse getDashboard() {
        long totalReviews     = reviewRepository.count();
        long pendingReports   = reportRepository.findByStatus(ReportStatus.PENDING, Pageable.unpaged()).getTotalElements();
        long pendingFeedbacks = analysisFeedbackRepository.findByStatus(AnalysisFeedbackStatus.SUBMITTED, Pageable.unpaged()).getTotalElements();
        long totalUsers       = userRepository.count();
        long suspicious       = reviewRepository.findSuspiciousReviews(50, Pageable.unpaged()).getTotalElements();
        long danger           = reviewRepository.findByTrustGradeWithProduct(TrustGrade.DANGER, Pageable.unpaged()).getTotalElements();
        return new AdminDashboardResponse(totalReviews, pendingReports, pendingFeedbacks, totalUsers, suspicious, danger);
    }

    // ── 의심 리뷰 관리 ────────────────────────────────────────────────────────

    public Page<AdminReviewResponse> getSuspiciousReviews(double maxRti, Pageable pageable) {
        return reviewRepository.findSuspiciousReviews(maxRti, pageable)
                .map(AdminReviewResponse::from);
    }

    // ── 신고 관리 ─────────────────────────────────────────────────────────────

    public Page<ReportResponse> getAllReports(ReportStatus status, Pageable pageable) {
        if (status != null) {
            return reportRepository.findByStatus(status, pageable).map(ReportResponse::from);
        }
        return reportRepository.findAllWithDetails(pageable).map(ReportResponse::from);
    }

    @Transactional
    public ReportResponse updateReportStatus(Long reportId, ReportStatus newStatus, String adminComment) {
        return reportService.updateReportStatus(reportId, newStatus, adminComment);
    }

    // ── 분석 피드백 검수 ──────────────────────────────────────────────────────

    public Page<AnalysisFeedbackResponse> getAllAnalysisFeedbacks(AnalysisFeedbackStatus status, Pageable pageable) {
        if (status != null) {
            return analysisFeedbackRepository.findByStatus(status, pageable).map(AnalysisFeedbackResponse::from);
        }
        return analysisFeedbackRepository.findAllWithDetails(pageable).map(AnalysisFeedbackResponse::from);
    }

    @Transactional
    public AnalysisFeedbackResponse reviewFeedback(Long feedbackId, AnalysisFeedbackStatus newStatus) {
        AnalysisFeedback feedback = analysisFeedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new CustomException(ErrorCode.FEEDBACK_NOT_FOUND));
        feedback.setStatus(newStatus);
        return AnalysisFeedbackResponse.from(analysisFeedbackRepository.save(feedback));
    }

    // ── 유저 관리 ─────────────────────────────────────────────────────────────

    public Page<AdminUserResponse> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(AdminUserResponse::from);
    }

    // ── 모델 성능 모니터링 ─────────────────────────────────────────────────────

    public AdminModelPerformanceResponse getModelPerformance(int days) {
        long total = reviewRepository.count();
        double avgRti = total == 0 ? 0.0
                : Math.round((reviewRepository.findOverallAverageRti() * 10.0)) / 10.0;

        // RTI 등급 분포
        long safeCount       = reviewRepository.countByTrustGrade(TrustGrade.SAFE);
        long suspiciousCount = reviewRepository.countByTrustGrade(TrustGrade.SUSPICIOUS);
        long dangerCount     = reviewRepository.countByTrustGrade(TrustGrade.DANGER);
        double safePercent       = total == 0 ? 0 : Math.round(safeCount       * 1000.0 / total) / 10.0;
        double suspiciousPercent = total == 0 ? 0 : Math.round(suspiciousCount * 1000.0 / total) / 10.0;
        double dangerPercent     = total == 0 ? 0 : Math.round(dangerCount     * 1000.0 / total) / 10.0;

        RtiDistribution rtiDist = new RtiDistribution(
                safeCount, suspiciousCount, dangerCount,
                safePercent, suspiciousPercent, dangerPercent
        );

        // 사용자 동의율
        long totalFeedbacks   = reviewFeedbackRepository.count();
        long agreementCount   = reviewFeedbackRepository.countAgreementFeedbacks();
        long disagreementCount = totalFeedbacks - agreementCount;
        double agreementRate  = totalFeedbacks == 0 ? 0.0
                : Math.round(agreementCount * 1000.0 / totalFeedbacks) / 10.0;

        UserAgreementStats agreement = new UserAgreementStats(
                totalFeedbacks, agreementCount, disagreementCount, agreementRate
        );

        // 분석 피드백 처리 현황
        long submitted   = analysisFeedbackRepository.countByStatus(AnalysisFeedbackStatus.SUBMITTED);
        long underReview = analysisFeedbackRepository.countByStatus(AnalysisFeedbackStatus.UNDER_REVIEW);
        long resolved    = analysisFeedbackRepository.countByStatus(AnalysisFeedbackStatus.RESOLVED);
        long rejected    = analysisFeedbackRepository.countByStatus(AnalysisFeedbackStatus.REJECTED);
        long fbTotal     = submitted + underReview + resolved + rejected;
        double resolutionRate = fbTotal == 0 ? 0.0
                : Math.round((resolved + rejected) * 1000.0 / fbTotal) / 10.0;

        AnalysisFeedbackStats fbStats = new AnalysisFeedbackStats(
                submitted, underReview, resolved, rejected, resolutionRate
        );

        // 일별 RTI 추이
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        List<DailyRtiTrend> trend = reviewRepository.findDailyRtiTrend(since)
                .stream()
                .map(p -> new DailyRtiTrend(
                        p.getReviewDate().toString(),
                        Math.round(p.getAvgRti() * 10.0) / 10.0,
                        p.getCnt()
                ))
                .toList();

        return new AdminModelPerformanceResponse(total, avgRti, rtiDist, agreement, fbStats, trend);
    }
}

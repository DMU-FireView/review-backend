package com.example.fireview.domain.report.service;

import com.example.fireview.domain.notification.entity.NotificationType;
import com.example.fireview.domain.notification.service.NotificationService;
import com.example.fireview.domain.report.dto.request.ReportCreateRequest;
import com.example.fireview.domain.report.dto.response.ReportResponse;
import com.example.fireview.domain.report.dto.response.ReportSummaryResponse;
import com.example.fireview.domain.report.entity.Report;
import com.example.fireview.domain.report.entity.ReportStatus;
import com.example.fireview.domain.report.repository.ReportRepository;
import com.example.fireview.domain.review.entity.Review;
import com.example.fireview.domain.review.repository.ReviewRepository;
import com.example.fireview.domain.user.entity.User;
import com.example.fireview.domain.user.service.UserService;
import com.example.fireview.global.exception.CustomException;
import com.example.fireview.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {

    private final ReportRepository reportRepository;
    private final ReviewRepository reviewRepository;
    private final UserService userService;
    private final NotificationService notificationService;

    // ── 일반 사용자 ──────────────────────────────────────────────────────────

    /** 리뷰 신고 제출 */
    @Transactional
    public ReportResponse createReport(Long reviewId, String userEmail,
                                       ReportCreateRequest request) {
        User reporter = userService.findByEmail(userEmail);
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new CustomException(ErrorCode.REVIEW_NOT_FOUND));

        if (reportRepository.existsByReporter_IdAndReview_Id(reporter.getId(), reviewId)) {
            throw new CustomException(ErrorCode.REPORT_ALREADY_EXISTS);
        }

        Report report = Report.builder()
                .reporter(reporter)
                .review(review)
                .reason(request.reason())
                .detail(request.detail())
                .build();

        Report saved = reportRepository.save(report);

        // 신고 접수 알림 발송
        String contentSummary = review.getContent() != null && review.getContent().length() > 30
                ? review.getContent().substring(0, 30) + "..."
                : review.getContent();
        notificationService.createNotification(
                reporter,
                NotificationType.REPORT_RECEIVED,
                "신고가 접수되었습니다",
                String.format("리뷰 '%s'에 대한 신고가 접수되었습니다. 검토 후 처리 결과를 알려드립니다.", contentSummary),
                "/reports/me/" + saved.getId()
        );

        return ReportResponse.from(saved);
    }

    /** 내 신고 목록 조회 (페이징) */
    public Page<ReportSummaryResponse> getMyReports(String userEmail, Pageable pageable) {
        User user = userService.findByEmail(userEmail);
        return reportRepository.findByReporterIdWithReview(user.getId(), pageable)
                .map(ReportSummaryResponse::from);
    }

    /** 내 신고 단건 조회 */
    public ReportResponse getMyReport(Long reportId, String userEmail) {
        User user = userService.findByEmail(userEmail);
        return reportRepository.findByIdAndReporter_Id(reportId, user.getId())
                .map(ReportResponse::from)
                .orElseThrow(() -> new CustomException(ErrorCode.REPORT_NOT_FOUND));
    }

    // ── 관리자 ───────────────────────────────────────────────────────────────

    /**
     * 신고 처리 상태 변경 (관리자 전용)
     * 상태 변경 시 신고자에게 알림 자동 발송
     */
    @Transactional
    public ReportResponse updateReportStatus(Long reportId, ReportStatus newStatus,
                                             String adminComment) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new CustomException(ErrorCode.REPORT_NOT_FOUND));

        report.setStatus(newStatus);
        report.setAdminComment(adminComment);

        // 상태별 알림 발송
        NotificationType notifType = switch (newStatus) {
            case UNDER_REVIEW -> NotificationType.REPORT_UNDER_REVIEW;
            case ACCEPTED     -> NotificationType.REPORT_ACCEPTED;
            case REJECTED     -> NotificationType.REPORT_REJECTED;
            default           -> null;
        };

        if (notifType != null) {
            String title = newStatus.getDescription();
            String message = buildStatusMessage(newStatus, report, adminComment);
            notificationService.createNotification(
                    report.getReporter(), notifType, title, message,
                    "/reports/me/" + report.getId()
            );
        }

        return ReportResponse.from(reportRepository.save(report));
    }

    private String buildStatusMessage(ReportStatus status, Report report, String comment) {
        String reviewSummary = report.getReview().getContent() != null
                && report.getReview().getContent().length() > 30
                ? report.getReview().getContent().substring(0, 30) + "..."
                : report.getReview().getContent();

        return switch (status) {
            case UNDER_REVIEW -> String.format("신고하신 리뷰 '%s'를 검토 중입니다.", reviewSummary);
            case ACCEPTED     -> String.format("신고하신 리뷰 '%s'의 처리가 완료되었습니다. %s",
                                    reviewSummary, comment != null ? comment : "");
            case REJECTED     -> String.format("신고하신 리뷰 '%s'는 기준에 해당하지 않아 기각되었습니다. %s",
                                    reviewSummary, comment != null ? comment : "");
            default           -> "";
        };
    }
}

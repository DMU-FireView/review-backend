package com.example.fireview.domain.report.service;

import com.example.fireview.domain.report.dto.request.ReportCreateRequest;
import com.example.fireview.domain.report.dto.response.ReportResponse;
import com.example.fireview.domain.report.dto.response.ReportSummaryResponse;
import com.example.fireview.domain.report.entity.Report;
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

        return ReportResponse.from(reportRepository.save(report));
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
}

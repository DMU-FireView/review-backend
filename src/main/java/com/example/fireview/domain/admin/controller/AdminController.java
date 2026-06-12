package com.example.fireview.domain.admin.controller;

import com.example.fireview.domain.admin.dto.request.AdminFeedbackReviewRequest;
import com.example.fireview.domain.admin.dto.request.AdminReportStatusUpdateRequest;
import com.example.fireview.domain.admin.dto.response.AdminDashboardResponse;
import com.example.fireview.domain.admin.dto.response.AdminModelPerformanceResponse;
import com.example.fireview.domain.admin.dto.response.AdminReviewResponse;
import com.example.fireview.domain.admin.dto.response.AdminUserResponse;
import com.example.fireview.domain.admin.service.AdminService;
import com.example.fireview.domain.feedback.dto.response.AnalysisFeedbackResponse;
import com.example.fireview.domain.feedback.entity.AnalysisFeedbackStatus;
import com.example.fireview.domain.report.dto.response.ReportResponse;
import com.example.fireview.domain.report.entity.ReportStatus;
import com.example.fireview.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    /** GET /api/admin/dashboard — 운영 대시보드 통계 */
    @GetMapping("/dashboard")
    public ApiResponse<AdminDashboardResponse> getDashboard() {
        return ApiResponse.success(adminService.getDashboard());
    }

    /** GET /api/admin/reviews/suspicious?maxRti=50 — 의심 리뷰 목록 */
    @GetMapping("/reviews/suspicious")
    public ApiResponse<Page<AdminReviewResponse>> getSuspiciousReviews(
            @RequestParam(defaultValue = "50") double maxRti,
            @PageableDefault(size = 20, sort = "rtiScore", direction = Sort.Direction.ASC)
            Pageable pageable) {
        return ApiResponse.success(adminService.getSuspiciousReviews(maxRti, pageable));
    }

    /** GET /api/admin/reports?status=PENDING — 전체 신고 목록 */
    @GetMapping("/reports")
    public ApiResponse<Page<ReportResponse>> getAllReports(
            @RequestParam(required = false) ReportStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return ApiResponse.success(adminService.getAllReports(status, pageable));
    }

    /** PATCH /api/admin/reports/{reportId} — 신고 상태 변경 */
    @PatchMapping("/reports/{reportId}")
    public ApiResponse<ReportResponse> updateReportStatus(
            @PathVariable Long reportId,
            @Valid @RequestBody AdminReportStatusUpdateRequest request) {
        return ApiResponse.success("신고 상태가 변경되었습니다.",
                adminService.updateReportStatus(reportId, request.status(), request.adminComment()));
    }

    /** GET /api/admin/analysis-feedbacks?status=SUBMITTED — 분석 피드백 검수 목록 */
    @GetMapping("/analysis-feedbacks")
    public ApiResponse<Page<AnalysisFeedbackResponse>> getAllAnalysisFeedbacks(
            @RequestParam(required = false) AnalysisFeedbackStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return ApiResponse.success(adminService.getAllAnalysisFeedbacks(status, pageable));
    }

    /** PATCH /api/admin/analysis-feedbacks/{feedbackId} — 분석 피드백 검수 처리 */
    @PatchMapping("/analysis-feedbacks/{feedbackId}")
    public ApiResponse<AnalysisFeedbackResponse> reviewFeedback(
            @PathVariable Long feedbackId,
            @Valid @RequestBody AdminFeedbackReviewRequest request) {
        return ApiResponse.success("피드백 검수가 완료되었습니다.",
                adminService.reviewFeedback(feedbackId, request.status()));
    }

    /** GET /api/admin/users — 전체 유저 목록 */
    @GetMapping("/users")
    public ApiResponse<Page<AdminUserResponse>> getAllUsers(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return ApiResponse.success(adminService.getAllUsers(pageable));
    }

    /**
     * GET /api/admin/model-performance?days=7 — AI 모델 성능 모니터링
     *
     * <p>DB 집계 기반 모델 성능 지표를 반환합니다.
     * <ul>
     *   <li>RTI 등급 분포 (SAFE / SUSPICIOUS / DANGER 비율)</li>
     *   <li>사용자 동의율 (AI 판정 vs REAL/FAKE 피드백 일치율)</li>
     *   <li>분석 피드백 처리 현황 (SUBMITTED / UNDER_REVIEW / RESOLVED / REJECTED)</li>
     *   <li>최근 N일 일별 평균 RTI 추이</li>
     * </ul>
     *
     * @param days 추이 조회 기간 (기본 7일, 최대 90일)
     */
    @GetMapping("/model-performance")
    public ApiResponse<AdminModelPerformanceResponse> getModelPerformance(
            @RequestParam(defaultValue = "7") int days) {
        if (days < 1)  days = 1;
        if (days > 90) days = 90;
        return ApiResponse.success(adminService.getModelPerformance(days));
    }
}

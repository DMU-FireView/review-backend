package com.example.fireview.domain.report.controller;

import com.example.fireview.domain.report.dto.request.ReportCreateRequest;
import com.example.fireview.domain.report.dto.response.ReportResponse;
import com.example.fireview.domain.report.dto.response.ReportSummaryResponse;
import com.example.fireview.domain.report.service.ReportService;
import com.example.fireview.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    /**
     * 리뷰 신고 제출
     * POST /api/reports/reviews/{reviewId}
     */
    @PostMapping("/reviews/{reviewId}")
    public ApiResponse<ReportResponse> createReport(
            @PathVariable Long reviewId,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ReportCreateRequest request) {
        ReportResponse response = reportService.createReport(reviewId, jwt.getSubject(), request);
        return ApiResponse.success("신고가 접수되었습니다.", response);
    }

    /**
     * 내 신고 목록 조회
     * GET /api/reports/me?page=0&size=10
     */
    @GetMapping("/me")
    public ApiResponse<Page<ReportSummaryResponse>> getMyReports(
            @AuthenticationPrincipal Jwt jwt,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        Page<ReportSummaryResponse> response = reportService.getMyReports(jwt.getSubject(), pageable);
        return ApiResponse.success(response);
    }

    /**
     * 내 신고 단건 조회
     * GET /api/reports/me/{reportId}
     */
    @GetMapping("/me/{reportId}")
    public ApiResponse<ReportResponse> getMyReport(
            @PathVariable Long reportId,
            @AuthenticationPrincipal Jwt jwt) {
        ReportResponse response = reportService.getMyReport(reportId, jwt.getSubject());
        return ApiResponse.success(response);
    }
}

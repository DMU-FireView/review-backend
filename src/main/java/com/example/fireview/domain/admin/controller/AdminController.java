package com.example.fireview.domain.admin.controller;

import com.example.fireview.domain.admin.dto.request.AdminReportStatusUpdateRequest;
import com.example.fireview.domain.admin.dto.response.AdminUserResponse;
import com.example.fireview.domain.admin.service.AdminService;
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

    /**
     * 전체 신고 목록 조회
     * GET /api/admin/reports?status=PENDING&page=0&size=20
     */
    @GetMapping("/reports")
    public ApiResponse<Page<ReportResponse>> getAllReports(
            @RequestParam(required = false) ReportStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return ApiResponse.success(adminService.getAllReports(status, pageable));
    }

    /**
     * 신고 상태 변경
     * PATCH /api/admin/reports/{reportId}
     */
    @PatchMapping("/reports/{reportId}")
    public ApiResponse<ReportResponse> updateReportStatus(
            @PathVariable Long reportId,
            @Valid @RequestBody AdminReportStatusUpdateRequest request) {
        return ApiResponse.success("신고 상태가 변경되었습니다.",
                adminService.updateReportStatus(reportId, request.status(), request.adminComment()));
    }

    /**
     * 전체 유저 목록 조회
     * GET /api/admin/users?page=0&size=20
     */
    @GetMapping("/users")
    public ApiResponse<Page<AdminUserResponse>> getAllUsers(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return ApiResponse.success(adminService.getAllUsers(pageable));
    }
}

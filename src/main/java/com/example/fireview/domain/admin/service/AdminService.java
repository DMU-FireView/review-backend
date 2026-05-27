package com.example.fireview.domain.admin.service;

import com.example.fireview.domain.admin.dto.response.AdminUserResponse;
import com.example.fireview.domain.report.dto.response.ReportResponse;
import com.example.fireview.domain.report.entity.ReportStatus;
import com.example.fireview.domain.report.repository.ReportRepository;
import com.example.fireview.domain.report.service.ReportService;
import com.example.fireview.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminService {

    private final ReportRepository reportRepository;
    private final ReportService reportService;
    private final UserRepository userRepository;

    /** 전체 신고 목록 (상태 필터 optional, 최신순) */
    public Page<ReportResponse> getAllReports(ReportStatus status, Pageable pageable) {
        if (status != null) {
            return reportRepository.findByStatus(status, pageable)
                    .map(ReportResponse::from);
        }
        return reportRepository.findAllWithDetails(pageable)
                .map(ReportResponse::from);
    }

    /** 신고 상태 변경 */
    @Transactional
    public ReportResponse updateReportStatus(Long reportId, ReportStatus newStatus, String adminComment) {
        return reportService.updateReportStatus(reportId, newStatus, adminComment);
    }

    /** 전체 유저 목록 */
    public Page<AdminUserResponse> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(AdminUserResponse::from);
    }
}

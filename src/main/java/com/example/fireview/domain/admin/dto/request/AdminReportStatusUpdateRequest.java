package com.example.fireview.domain.admin.dto.request;

import com.example.fireview.domain.report.entity.ReportStatus;
import jakarta.validation.constraints.NotNull;

public record AdminReportStatusUpdateRequest(
        @NotNull ReportStatus status,
        String adminComment
) {}

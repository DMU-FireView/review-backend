package com.example.fireview.domain.user.dto;

public record UserSettingUpdateRequest(
        Boolean notifyReportResult,
        Boolean notifyFeedback,
        Boolean notifyMarketing
) {}

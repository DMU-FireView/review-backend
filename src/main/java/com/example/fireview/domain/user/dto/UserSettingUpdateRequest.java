package com.example.fireview.domain.user.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record UserSettingUpdateRequest(
        // 알림 설정
        Boolean notifyRiskyProduct,
        Boolean notifyAnalysisComplete,
        Boolean notifyFeedbackResult,
        Boolean notifyMarketing,
        // 신뢰 필터
        @Min(0) @Max(100) Integer rtiThreshold,
        Boolean hideRiskyReviews,
        Boolean showSuspiciousLabel,
        Boolean prioritizeVerifiedReviews,
        Boolean autoOpenAnalysisPopup,
        // 화면 경험
        String cardDensity,
        String reviewSortOrder,
        String rtiLabelStyle,
        String theme,
        // 개인정보
        Boolean allowDataAnalysis
) {}

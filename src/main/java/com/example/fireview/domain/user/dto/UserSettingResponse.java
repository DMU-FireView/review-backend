package com.example.fireview.domain.user.dto;

import com.example.fireview.domain.user.entity.UserSetting;

public record UserSettingResponse(
        // 알림 설정
        boolean notifyRiskyProduct,
        boolean notifyAnalysisComplete,
        boolean notifyFeedbackResult,
        boolean notifyMarketing,
        // 신뢰 필터
        int rtiThreshold,
        boolean hideRiskyReviews,
        boolean showSuspiciousLabel,
        boolean prioritizeVerifiedReviews,
        boolean autoOpenAnalysisPopup,
        // 화면 경험
        String cardDensity,
        String reviewSortOrder,
        String rtiLabelStyle,
        String theme,
        // 개인정보
        boolean allowDataAnalysis
) {
    public static UserSettingResponse from(UserSetting s) {
        return new UserSettingResponse(
                s.isNotifyRiskyProduct(),
                s.isNotifyAnalysisComplete(),
                s.isNotifyFeedbackResult(),
                s.isNotifyMarketing(),
                s.getRtiThreshold(),
                s.isHideRiskyReviews(),
                s.isShowSuspiciousLabel(),
                s.isPrioritizeVerifiedReviews(),
                s.isAutoOpenAnalysisPopup(),
                s.getCardDensity(),
                s.getReviewSortOrder(),
                s.getRtiLabelStyle(),
                s.getTheme(),
                s.isAllowDataAnalysis()
        );
    }

    public static UserSettingResponse defaults() {
        return new UserSettingResponse(
                true, true, true, false,
                50, true, true, true, false,
                "COMFORTABLE", "VERIFIED_RECENT", "BADGE_SMALL", "SYSTEM",
                true
        );
    }
}

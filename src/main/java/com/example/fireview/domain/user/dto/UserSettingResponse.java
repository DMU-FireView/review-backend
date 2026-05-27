package com.example.fireview.domain.user.dto;

import com.example.fireview.domain.user.entity.UserSetting;

public record UserSettingResponse(
        boolean notifyReportResult,
        boolean notifyFeedback,
        boolean notifyMarketing
) {
    public static UserSettingResponse from(UserSetting setting) {
        return new UserSettingResponse(
                setting.isNotifyReportResult(),
                setting.isNotifyFeedback(),
                setting.isNotifyMarketing()
        );
    }

    /** 아직 설정 레코드가 없을 때 기본값 반환 */
    public static UserSettingResponse defaults() {
        return new UserSettingResponse(true, true, false);
    }
}

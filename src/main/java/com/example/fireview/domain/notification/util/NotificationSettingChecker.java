package com.example.fireview.domain.notification.util;

import com.example.fireview.domain.user.entity.UserSetting;
import lombok.extern.slf4j.Slf4j;

/**
 * UserSetting 알림 플래그 확인 유틸리티.
 * NotificationType.settingKey → UserSetting 필드값으로 변환한다.
 */
@Slf4j
public final class NotificationSettingChecker {

    private NotificationSettingChecker() {}

    /**
     * settingKey 에 해당하는 UserSetting 필드값을 반환한다.
     * 알 수 없는 키는 true(발송 허용)를 반환한다.
     *
     * @param setting    사용자 알림 설정
     * @param settingKey NotificationType.settingKey
     * @return true 이면 발송, false 이면 차단
     */
    public static boolean isEnabled(UserSetting setting, String settingKey) {
        return switch (settingKey) {
            case "notifyFeedbackResult"   -> setting.isNotifyFeedbackResult();
            case "notifyAnalysisComplete" -> setting.isNotifyAnalysisComplete();
            case "notifyRiskyProduct"     -> setting.isNotifyRiskyProduct();
            case "notifyMarketing"        -> setting.isNotifyMarketing();
            default -> {
                log.warn("[Notification] 알 수 없는 settingKey: {}", settingKey);
                yield true;
            }
        };
    }
}

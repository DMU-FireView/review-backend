package com.example.fireview.domain.notification.service;

import com.example.fireview.domain.notification.dto.response.NotificationResponse;
import com.example.fireview.domain.notification.dto.response.UnreadCountResponse;
import com.example.fireview.domain.notification.entity.Notification;
import com.example.fireview.domain.notification.entity.NotificationType;
import com.example.fireview.domain.notification.repository.NotificationRepository;
import com.example.fireview.domain.notification.util.NotificationSettingChecker;
import com.example.fireview.domain.user.entity.User;
import com.example.fireview.domain.user.repository.UserSettingRepository;
import com.example.fireview.domain.user.service.UserService;
import com.example.fireview.global.exception.CustomException;
import com.example.fireview.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserSettingRepository userSettingRepository;
    private final UserService userService;

    // ── 조회 ─────────────────────────────────────────────────────────────────

    /** 내 알림 목록 (페이징) */
    public Page<NotificationResponse> getMyNotifications(String userEmail, Pageable pageable) {
        User user = userService.findByEmail(userEmail);
        return notificationRepository
                .findByReceiver_IdOrderByCreatedAtDesc(user.getId(), pageable)
                .map(NotificationResponse::from);
    }

    /** 읽지 않은 알림 수 */
    public UnreadCountResponse getUnreadCount(String userEmail) {
        User user = userService.findByEmail(userEmail);
        long count = notificationRepository.countByReceiver_IdAndIsReadFalse(user.getId());
        return new UnreadCountResponse(count);
    }

    // ── 읽음 처리 ─────────────────────────────────────────────────────────────

    /** 단건 읽음 처리 */
    @Transactional
    public void markAsRead(Long notificationId, String userEmail) {
        User user = userService.findByEmail(userEmail);
        Notification notification = notificationRepository
                .findByIdAndReceiver_Id(notificationId, user.getId())
                .orElseThrow(() -> new CustomException(ErrorCode.NOTIFICATION_NOT_FOUND));
        notification.markAsRead();
    }

    /** 전체 읽음 처리 */
    @Transactional
    public void markAllAsRead(String userEmail) {
        User user = userService.findByEmail(userEmail);
        notificationRepository.markAllAsRead(user.getId());
    }

    // ── 알림 생성 (내부 호출용) ───────────────────────────────────────────────

    /**
     * 알림 생성 - 다른 서비스에서 이벤트 발생 시 호출.
     * NotificationType.settingKey 에 대응하는 UserSetting 필드가 false 이면 발송하지 않는다.
     *
     * @param receiver   수신자
     * @param type       알림 유형
     * @param title      알림 제목
     * @param message    알림 본문
     * @param targetUrl  클릭 시 이동 경로 (nullable)
     */
    @Transactional
    public void createNotification(User receiver, NotificationType type,
                                   String title, String message, String targetUrl) {

        // UserSetting 알림 off 체크 (settingKey 없으면 항상 발송)
        if (type.getSettingKey() != null) {
            boolean enabled = userSettingRepository.findByUser_Id(receiver.getId())
                    .map(s -> NotificationSettingChecker.isEnabled(s, type.getSettingKey()))
                    .orElse(true); // 설정 미등록 시 기본값 true

            if (!enabled) {
                log.debug("[Notification] 알림 설정 off - userId={}, type={}", receiver.getId(), type);
                return;
            }
        }

        Notification notification = Notification.builder()
                .receiver(receiver)
                .type(type)
                .title(title)
                .message(message)
                .targetUrl(targetUrl)
                .build();
        notificationRepository.save(notification);
        log.debug("[Notification] 알림 저장 - userId={}, type={}", receiver.getId(), type);
    }
}

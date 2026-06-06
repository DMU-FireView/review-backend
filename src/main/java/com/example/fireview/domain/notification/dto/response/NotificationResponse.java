package com.example.fireview.domain.notification.dto.response;

import com.example.fireview.domain.notification.entity.Notification;
import com.example.fireview.domain.notification.entity.NotificationType;

import java.time.LocalDateTime;

public record NotificationResponse(
        Long notificationId,
        NotificationType type,
        String typeDescription,
        String title,
        String message,
        boolean isRead,
        String targetUrl,
        LocalDateTime createdAt
) {
    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getType(),
                notification.getType().getDescription(),
                notification.getTitle(),
                notification.getMessage(),
                notification.isRead(),
                notification.getTargetUrl(),
                notification.getCreatedAt()
        );
    }
}

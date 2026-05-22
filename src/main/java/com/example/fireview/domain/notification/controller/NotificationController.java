package com.example.fireview.domain.notification.controller;

import com.example.fireview.domain.notification.dto.response.NotificationResponse;
import com.example.fireview.domain.notification.dto.response.UnreadCountResponse;
import com.example.fireview.domain.notification.service.NotificationService;
import com.example.fireview.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * 내 알림 목록 조회
     * GET /api/notifications/me?page=0&size=20
     */
    @GetMapping("/me")
    public ApiResponse<Page<NotificationResponse>> getMyNotifications(
            @AuthenticationPrincipal Jwt jwt,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return ApiResponse.success(
                notificationService.getMyNotifications(jwt.getSubject(), pageable));
    }

    /**
     * 읽지 않은 알림 수 조회 (상단바 뱃지)
     * GET /api/notifications/me/unread-count
     */
    @GetMapping("/me/unread-count")
    public ApiResponse<UnreadCountResponse> getUnreadCount(
            @AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.success(
                notificationService.getUnreadCount(jwt.getSubject()));
    }

    /**
     * 단건 읽음 처리
     * PATCH /api/notifications/{notificationId}/read
     */
    @PatchMapping("/{notificationId}/read")
    public ApiResponse<Void> markAsRead(
            @PathVariable Long notificationId,
            @AuthenticationPrincipal Jwt jwt) {
        notificationService.markAsRead(notificationId, jwt.getSubject());
        return ApiResponse.ok("알림을 읽음 처리했습니다.");
    }

    /**
     * 전체 읽음 처리
     * PATCH /api/notifications/me/read-all
     */
    @PatchMapping("/me/read-all")
    public ApiResponse<Void> markAllAsRead(
            @AuthenticationPrincipal Jwt jwt) {
        notificationService.markAllAsRead(jwt.getSubject());
        return ApiResponse.ok("모든 알림을 읽음 처리했습니다.");
    }
}

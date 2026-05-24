package com.example.fireview.domain.user.dto;

/**
 * 마이페이지 이용 통계 응답 DTO
 */
public record UserStatsResponse(
        long wishlistCount,
        long feedbackCount,
        long reportCount,
        long unreadNotificationCount
) {}

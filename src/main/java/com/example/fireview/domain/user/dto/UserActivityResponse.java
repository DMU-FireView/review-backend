package com.example.fireview.domain.user.dto;

import java.time.LocalDateTime;

public record UserActivityResponse(
        String activityType,   // WISHLIST_ADD / FEEDBACK_SUBMIT / SEARCH
        String description,
        String targetId,
        LocalDateTime createdAt
) {}

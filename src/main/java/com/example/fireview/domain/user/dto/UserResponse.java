package com.example.fireview.domain.user.dto;

import com.example.fireview.domain.user.entity.Role;
import com.example.fireview.domain.user.entity.User;

import java.time.LocalDateTime;

public record UserResponse(
        Long id,
        String email,
        String nickname,
        Role role,
        LocalDateTime createdAt,
        boolean onboardingCompleted
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getRole(),
                user.getCreatedAt(),
                user.isOnboardingCompleted()
        );
    }
}
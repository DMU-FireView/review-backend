package com.example.fireview.domain.auth.dto;

import com.example.fireview.domain.user.entity.Role;

public record LoginResponse(
        String accessToken,
        String tokenType,
        String email,
        String nickname,
        Role role,
        boolean onboardingCompleted
) {
    public LoginResponse(String accessToken, String email, String nickname, Role role, boolean onboardingCompleted) {
        this(accessToken, "Bearer", email, nickname, role, onboardingCompleted);
    }
}
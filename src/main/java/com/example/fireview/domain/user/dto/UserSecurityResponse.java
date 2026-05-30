package com.example.fireview.domain.user.dto;

import com.example.fireview.domain.user.entity.OAuthProvider;
import com.example.fireview.domain.user.entity.User;

import java.time.LocalDateTime;

public record UserSecurityResponse(
        boolean emailVerified,
        boolean twoFactorEnabled,
        OAuthProvider loginMethod,
        LocalDateTime passwordLastChanged,
        boolean termsAgreed,
        boolean notificationPermissionGranted
) {
    public static UserSecurityResponse from(User user) {
        boolean isLocal = user.getProvider() == OAuthProvider.LOCAL;
        return new UserSecurityResponse(
                true,
                false,
                user.getProvider(),
                isLocal ? user.getCreatedAt() : null,
                true,
                true
        );
    }
}

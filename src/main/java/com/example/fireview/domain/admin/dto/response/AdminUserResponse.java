package com.example.fireview.domain.admin.dto.response;

import com.example.fireview.domain.user.entity.OAuthProvider;
import com.example.fireview.domain.user.entity.Role;
import com.example.fireview.domain.user.entity.User;

import java.time.LocalDateTime;

public record AdminUserResponse(
        Long userId,
        String email,
        String nickname,
        Role role,
        OAuthProvider provider,
        Double atiScore,
        LocalDateTime createdAt
) {
    public static AdminUserResponse from(User user) {
        return new AdminUserResponse(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getRole(),
                user.getProvider(),
                user.getAtiScore(),
                user.getCreatedAt()
        );
    }
}

package com.example.fireview.domain.user.dto;

import com.example.fireview.domain.user.entity.OAuthProvider;
import com.example.fireview.domain.user.entity.Role;
import com.example.fireview.domain.user.entity.User;

import java.time.LocalDateTime;
import java.util.List;

public record UserResponse(
        Long id,
        String email,
        String nickname,
        String profileImageUrl,
        Role role,
        OAuthProvider provider,
        Double atiScore,
        LocalDateTime createdAt,
        boolean onboardingCompleted,
        String phone,
        List<String> interestCategories
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getProfileImageUrl(),
                user.getRole(),
                user.getProvider(),
                user.getAtiScore(),
                user.getCreatedAt(),
                user.isOnboardingCompleted(),
                user.getPhone(),
                user.getInterestCategories()
        );
    }
}

package com.example.fireview.domain.admin.dto.response;

import com.example.fireview.domain.user.entity.OAuthProvider;
import com.example.fireview.domain.user.entity.PlanTier;
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
        LocalDateTime createdAt,
        PlanTier planTier,
        LocalDateTime planExpiresAt
) {
    public static AdminUserResponse from(User user) {
        return new AdminUserResponse(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getRole(),
                user.getProvider(),
                user.getAtiScore(),
                user.getCreatedAt(),
                // 저장된 값 그대로 준다. 만료돼 FREE 로 동작 중인 상태도 운영자는
                // 원래 등급과 만료 시각을 봐야 환불·연장을 판단할 수 있다.
                user.getPlanTier(),
                user.getPlanExpiresAt()
        );
    }
}

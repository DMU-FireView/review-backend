package com.example.fireview.domain.user.dto;

import com.example.fireview.domain.user.entity.OAuthProvider;
import com.example.fireview.domain.user.entity.PlanTier;
import com.example.fireview.domain.user.entity.Role;
import com.example.fireview.domain.user.entity.User;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @param planTier      지금 적용 중인 챗봇 요금제. 만료된 유료 요금제는 FREE 로 내려온다.
 *                      남은 사용량까지 필요하면 {@code GET /api/chat/quota} 를 쓴다
 * @param planExpiresAt 요금제 만료 시각. null 이면 만료 없음
 */
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
        List<String> interestCategories,
        PlanTier planTier,
        LocalDateTime planExpiresAt
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
                user.getInterestCategories(),
                user.getEffectivePlan(),
                user.getPlanExpiresAt()
        );
    }
}

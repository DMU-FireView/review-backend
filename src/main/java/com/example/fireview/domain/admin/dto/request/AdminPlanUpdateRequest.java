package com.example.fireview.domain.admin.dto.request;

import com.example.fireview.domain.user.entity.PlanTier;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/**
 * @param planTier  바꿀 요금제
 * @param expiresAt 만료 시각. 비우면 무기한. FREE 로 내리면 무시된다
 */
public record AdminPlanUpdateRequest(
        @NotNull PlanTier planTier,
        LocalDateTime expiresAt
) {}

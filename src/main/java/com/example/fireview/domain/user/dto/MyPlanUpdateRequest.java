package com.example.fireview.domain.user.dto;

import com.example.fireview.domain.user.entity.PlanTier;
import jakarta.validation.constraints.NotNull;

/**
 * @param planTier 바꿀 요금제. FREE / PLUS / PRO 외의 값은 400 이다
 */
public record MyPlanUpdateRequest(
        @NotNull(message = "요금제를 선택해주세요.")
        PlanTier planTier
) {}

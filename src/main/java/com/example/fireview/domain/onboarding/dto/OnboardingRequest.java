package com.example.fireview.domain.onboarding.dto;

import com.example.fireview.domain.product.entity.Category;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.Set;

public record OnboardingRequest(
        @NotNull(message = "관심 카테고리를 선택해주세요.")
        Set<Category> preferredCategories,

        @Min(value = 0, message = "신뢰 필터 최솟값은 0 이상이어야 합니다.")
        @Max(value = 100, message = "신뢰 필터 최솟값은 100 이하여야 합니다.")
        int minTrustScore
) {}
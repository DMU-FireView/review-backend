package com.example.fireview.domain.onboarding.dto;

import com.example.fireview.domain.product.entity.Category;
import com.example.fireview.domain.onboarding.entity.UserPreference;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

public record OnboardingResponse(
        Set<Category> preferredCategories,
        int minTrustScore,
        List<CategoryInfo> availableCategories
) {
    public static OnboardingResponse from(UserPreference pref) {
        return new OnboardingResponse(
                pref.getPreferredCategories(),
                pref.getMinTrustScore(),
                allCategories()
        );
    }

    public static List<CategoryInfo> allCategories() {
        return Arrays.stream(Category.values())
                .map(c -> new CategoryInfo(c, c.getDisplayName()))
                .toList();
    }

    public record CategoryInfo(Category value, String displayName) {}
}
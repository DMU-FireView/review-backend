package com.example.fireview.domain.onboarding.service;

import com.example.fireview.domain.onboarding.dto.OnboardingRequest;
import com.example.fireview.domain.onboarding.dto.OnboardingResponse;
import com.example.fireview.domain.onboarding.entity.UserPreference;
import com.example.fireview.domain.onboarding.repository.UserPreferenceRepository;
import com.example.fireview.domain.user.entity.User;
import com.example.fireview.domain.user.service.UserService;
import com.example.fireview.global.exception.CustomException;
import com.example.fireview.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

import com.example.fireview.domain.product.entity.Category;
import com.example.fireview.domain.onboarding.dto.OnboardingResponse.CategoryInfo;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OnboardingService {

    private final UserPreferenceRepository preferenceRepository;
    private final UserService userService;

    public List<CategoryInfo> getAvailableCategories() {
        return OnboardingResponse.allCategories();
    }

    public OnboardingResponse getPreferences(String userEmail) {
        User user = userService.findByEmail(userEmail);
        UserPreference pref = preferenceRepository.findByUser_Id(user.getId())
                .orElse(UserPreference.builder().user(user).build());
        return OnboardingResponse.from(pref);
    }

    @Transactional
    public OnboardingResponse savePreferences(String userEmail, OnboardingRequest request) {
        User user = userService.findByEmail(userEmail);

        UserPreference pref = preferenceRepository.findByUser_Id(user.getId())
                .orElse(UserPreference.builder().user(user).build());

        pref.setPreferredCategories(request.preferredCategories());
        pref.setMinTrustScore(request.minTrustScore());
        UserPreference saved = preferenceRepository.save(pref);

        if (!user.isOnboardingCompleted()) {
            userService.markOnboardingComplete(user.getId());
        }

        return OnboardingResponse.from(saved);
    }
}
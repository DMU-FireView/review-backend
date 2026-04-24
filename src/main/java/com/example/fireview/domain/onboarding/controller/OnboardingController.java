package com.example.fireview.domain.onboarding.controller;

import com.example.fireview.domain.onboarding.dto.OnboardingRequest;
import com.example.fireview.domain.onboarding.dto.OnboardingResponse;
import com.example.fireview.domain.onboarding.dto.OnboardingResponse.CategoryInfo;
import com.example.fireview.domain.onboarding.service.OnboardingService;
import com.example.fireview.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/onboarding")
@RequiredArgsConstructor
public class OnboardingController {

    private final OnboardingService onboardingService;

    @GetMapping("/categories")
    public ApiResponse<List<CategoryInfo>> getCategories() {
        return ApiResponse.success(onboardingService.getAvailableCategories());
    }

    @GetMapping("/preferences")
    public ApiResponse<OnboardingResponse> getPreferences(@AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.success(onboardingService.getPreferences(jwt.getSubject()));
    }

    @PostMapping("/preferences")
    public ApiResponse<OnboardingResponse> savePreferences(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody OnboardingRequest request) {
        return ApiResponse.success(onboardingService.savePreferences(jwt.getSubject(), request));
    }
}
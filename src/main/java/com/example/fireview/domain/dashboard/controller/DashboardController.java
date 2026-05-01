package com.example.fireview.domain.dashboard.controller;

import com.example.fireview.domain.dashboard.dto.DashboardResponse;
import com.example.fireview.domain.dashboard.service.DashboardService;
import com.example.fireview.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    public ApiResponse<DashboardResponse> getDashboard(@AuthenticationPrincipal Jwt jwt) {
        // jwt == null → 비회원: 개인화 없는 공개 대시보드 반환
        // jwt != null → 로그인 사용자: 온보딩 카테고리 기반 개인화 대시보드 반환
        if (jwt == null) {
            return ApiResponse.success(dashboardService.getPublicDashboard());
        }
        return ApiResponse.success(dashboardService.getDashboard(jwt.getSubject()));
    }

    @GetMapping("/keywords")
    public ApiResponse<List<String>> getPopularKeywords() {
        return ApiResponse.success(dashboardService.getPopularKeywords());
    }
}
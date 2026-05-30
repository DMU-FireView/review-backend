package com.example.fireview.domain.user.controller;

import com.example.fireview.domain.user.dto.*;
import com.example.fireview.domain.user.service.UserService;
import com.example.fireview.domain.user.service.UserSettingService;
import com.example.fireview.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserSettingService userSettingService;

    /** GET /api/users/me — 내 프로필 조회 */
    @GetMapping("/me")
    public ApiResponse<UserResponse> getMyProfile(@AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.success(userService.getProfile(jwt.getSubject()));
    }

    /** GET /api/users/me/stats — 이용 통계 */
    @GetMapping("/me/stats")
    public ApiResponse<UserStatsResponse> getMyStats(@AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.success(userService.getStats(jwt.getSubject()));
    }

    /** PATCH /api/users/me — 프로필 수정 (닉네임, 이미지, 전화번호, 관심 카테고리) */
    @PatchMapping("/me")
    public ApiResponse<UserResponse> updateMyProfile(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ProfileUpdateRequest request) {
        return ApiResponse.success("프로필이 수정되었습니다.",
                userService.updateProfile(jwt.getSubject(), request));
    }

    /** DELETE /api/users/me — 회원 탈퇴 */
    @DeleteMapping("/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMyAccount(@AuthenticationPrincipal Jwt jwt) {
        userService.deleteAccount(jwt.getSubject());
    }

    /** GET /api/users/me/activities — 최근 활동 목록 */
    @GetMapping("/me/activities")
    public ApiResponse<List<UserActivityResponse>> getMyActivities(@AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.success(userService.getRecentActivities(jwt.getSubject()));
    }

    /** GET /api/users/me/security — 보안 상태 조회 */
    @GetMapping("/me/security")
    public ApiResponse<UserSecurityResponse> getMySecurityStatus(@AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.success(userService.getSecurityStatus(jwt.getSubject()));
    }

    /** GET /api/users/me/settings — 설정 조회 */
    @GetMapping("/me/settings")
    public ApiResponse<UserSettingResponse> getMySettings(@AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.success(userSettingService.getSettings(jwt.getSubject()));
    }

    /** PATCH /api/users/me/settings — 설정 변경 */
    @PatchMapping("/me/settings")
    public ApiResponse<UserSettingResponse> updateMySettings(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UserSettingUpdateRequest request) {
        return ApiResponse.success("설정이 변경되었습니다.",
                userSettingService.updateSettings(jwt.getSubject(), request));
    }
}

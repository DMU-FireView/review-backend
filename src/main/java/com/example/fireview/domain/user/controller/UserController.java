package com.example.fireview.domain.user.controller;

import com.example.fireview.domain.user.dto.ProfileUpdateRequest;
import com.example.fireview.domain.user.dto.UserResponse;
import com.example.fireview.domain.user.dto.UserStatsResponse;
import com.example.fireview.domain.user.service.UserService;
import com.example.fireview.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 내 프로필 조회
     * GET /api/users/me
     */
    @GetMapping("/me")
    public ApiResponse<UserResponse> getMyProfile(@AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.success(userService.getProfile(jwt.getSubject()));
    }

    /**
     * 내 이용 통계 조회
     * GET /api/users/me/stats
     */
    @GetMapping("/me/stats")
    public ApiResponse<UserStatsResponse> getMyStats(@AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.success(userService.getStats(jwt.getSubject()));
    }

    /**
     * 프로필 수정 (닉네임, 프로필 이미지)
     * PATCH /api/users/me
     */
    @PatchMapping("/me")
    public ApiResponse<UserResponse> updateMyProfile(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ProfileUpdateRequest request) {
        return ApiResponse.success("프로필이 수정되었습니다.",
                userService.updateProfile(jwt.getSubject(), request));
    }

    /**
     * 회원 탈퇴
     * DELETE /api/users/me
     */
    @DeleteMapping("/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMyAccount(@AuthenticationPrincipal Jwt jwt) {
        userService.deleteAccount(jwt.getSubject());
    }
}

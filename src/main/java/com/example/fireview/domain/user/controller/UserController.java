package com.example.fireview.domain.user.controller;

import com.example.fireview.domain.user.dto.UserResponse;
import com.example.fireview.domain.user.service.UserService;
import com.example.fireview.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ApiResponse<UserResponse> getMyProfile(@AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.success(userService.getProfile(jwt.getSubject()));
    }
}
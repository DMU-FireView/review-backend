package com.example.fireview.domain.auth.dto;

import com.example.fireview.domain.user.entity.Role;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 로그인·회원가입·refresh 응답 본문.
 *
 * <p>{@code refreshToken} 은 모바일 앱({@code X-Client-Platform: app})에만 채운다.
 * 웹은 HttpOnly 쿠키로만 받으므로 null 이고, null 이면 JSON 에서 아예 빠진다.
 */
public record LoginResponse(
        String accessToken,
        String tokenType,
        String email,
        String nickname,
        Role role,
        boolean onboardingCompleted,
        @JsonInclude(JsonInclude.Include.NON_NULL) String refreshToken
) {
    public LoginResponse(String accessToken, String tokenType, String email, String nickname,
                         Role role, boolean onboardingCompleted) {
        this(accessToken, tokenType, email, nickname, role, onboardingCompleted, null);
    }

    public LoginResponse(String accessToken, String email, String nickname, Role role, boolean onboardingCompleted) {
        this(accessToken, "Bearer", email, nickname, role, onboardingCompleted);
    }

    public LoginResponse withRefreshToken(String token) {
        return new LoginResponse(accessToken, tokenType, email, nickname, role, onboardingCompleted, token);
    }
}

package com.example.fireview.domain.auth.controller;

import com.example.fireview.domain.auth.dto.LoginRequest;
import com.example.fireview.domain.auth.dto.LoginResponse;
import com.example.fireview.domain.auth.dto.PasswordResetRequest;
import com.example.fireview.domain.auth.dto.SignupRequest;
import com.example.fireview.domain.auth.service.AuthService;
import com.example.fireview.global.response.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Void> signup(@Valid @RequestBody SignupRequest request) {
        authService.signup(request);
        return ApiResponse.success("회원가입이 완료되었습니다.", null);
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(authService.login(request));
    }

    @PostMapping("/password/reset-request")
    public ApiResponse<Map<String, String>> requestPasswordReset(@RequestParam @NotBlank @Email String email) {
        String token = authService.requestPasswordReset(email);
        // In production this token is emailed to the user
        return ApiResponse.success("비밀번호 재설정 토큰이 발급되었습니다.", Map.of("resetToken", token));
    }

    @PostMapping("/password/reset")
    public ApiResponse<Void> resetPassword(@Valid @RequestBody PasswordResetRequest request) {
        authService.resetPassword(request);
        return ApiResponse.ok("비밀번호가 변경되었습니다.");
    }
}
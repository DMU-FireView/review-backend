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

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<LoginResponse> signup(@Valid @RequestBody SignupRequest request) {
        LoginResponse response = authService.signup(request);
        return ApiResponse.success("회원가입이 완료되었습니다.", response);
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(authService.login(request));
    }

    /**
     * 비밀번호 재설정 요청.
     * 토큰을 응답으로 반환하지 않고 등록된 이메일로 재설정 링크를 발송한다.
     */
    @PostMapping("/password/reset-request")
    public ApiResponse<Void> requestPasswordReset(@RequestParam @NotBlank @Email String email) {
        authService.requestPasswordReset(email);
        return ApiResponse.ok("비밀번호 재설정 링크가 이메일로 발송되었습니다.");
    }

    @PostMapping("/password/reset")
    public ApiResponse<Void> resetPassword(@Valid @RequestBody PasswordResetRequest request) {
        authService.resetPassword(request);
        return ApiResponse.ok("비밀번호가 변경되었습니다.");
    }
}

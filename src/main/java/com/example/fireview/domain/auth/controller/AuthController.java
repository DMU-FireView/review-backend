package com.example.fireview.domain.auth.controller;

import com.example.fireview.domain.auth.cookie.RefreshTokenCookies;
import com.example.fireview.domain.auth.dto.AuthResult;
import com.example.fireview.domain.auth.dto.LoginRequest;
import com.example.fireview.domain.auth.dto.LoginResponse;
import com.example.fireview.domain.auth.dto.PasswordResetRequest;
import com.example.fireview.domain.auth.dto.RefreshTokenRequest;
import com.example.fireview.domain.auth.dto.SignupRequest;
import com.example.fireview.domain.auth.service.AuthService;
import com.example.fireview.domain.auth.service.InvalidRefreshTokenException;
import com.example.fireview.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "인증", description = "회원가입·로그인·세션 연장·로그아웃·비밀번호 재설정")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    /** 앱은 쿠키 저장소가 없어서 이 헤더로 리프레시 토큰을 본문으로 받겠다고 알린다 */
    static final String CLIENT_PLATFORM_HEADER = "X-Client-Platform";
    static final String APP_PLATFORM = "app";

    private final AuthService authService;
    private final RefreshTokenCookies refreshTokenCookies;

    @Operation(summary = "회원가입", description = """
            성공하면 리프레시 토큰을 HttpOnly 쿠키(Set-Cookie)로 내려준다.
            X-Client-Platform: app 이면 본문 data.refreshToken 에도 싣는다(웹에는 절대 싣지 않는다).
            """)
    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<LoginResponse> signup(@Valid @RequestBody SignupRequest request,
                                             @Parameter(description = "앱이면 app — 본문으로 refreshToken 을 받는다") @RequestHeader(value = CLIENT_PLATFORM_HEADER, required = false) String platform,
                                             HttpServletResponse response) {
        LoginResponse body = withSession(authService.signup(request), platform, response);
        return ApiResponse.success("회원가입이 완료되었습니다.", body);
    }

    @Operation(summary = "로그인", description = """
            성공하면 리프레시 토큰을 HttpOnly 쿠키(Set-Cookie)로 내려준다.
            X-Client-Platform: app 이면 본문 data.refreshToken 에도 싣는다(웹에는 절대 싣지 않는다).
            """)
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request,
                                            @Parameter(description = "앱이면 app — 본문으로 refreshToken 을 받는다") @RequestHeader(value = CLIENT_PLATFORM_HEADER, required = false) String platform,
                                            HttpServletResponse response) {
        return ApiResponse.success(withSession(authService.login(request), platform, response));
    }

    @Operation(summary = "세션 연장(액세스 토큰 재발급)", description = """
            리프레시 토큰(쿠키 review_rt, 없으면 앱용 본문 refreshToken)을 검증해 새 액세스 토큰을 발급하고
            리프레시 토큰을 회전한다(쿠키 갱신). 응답 본문은 로그인과 같은 모양이다.
            30분 동안 refresh 가 없으면 만료된다. 실패하면 401 REFRESH_TOKEN_INVALID 와 함께 쿠키를 지운다 —
            프론트는 재시도하지 말고 로그아웃 처리할 것. 여러 탭이 동시에 불러도 15초 안이면 모두 성공한다.
            """)
    @PostMapping("/refresh")
    public ApiResponse<LoginResponse> refresh(@RequestBody(required = false) RefreshTokenRequest request,
                                              @Parameter(description = "앱이면 app — 본문으로 refreshToken 을 받는다") @RequestHeader(value = CLIENT_PLATFORM_HEADER, required = false) String platform,
                                              HttpServletRequest httpRequest,
                                              HttpServletResponse response) {
        String token = presentedToken(request, httpRequest);
        return ApiResponse.success(withSession(authService.refresh(token), platform, response));
    }

    @Operation(summary = "로그아웃", description = """
            리프레시 토큰을 폐기하고 쿠키를 지운다. 토큰이 없거나 이미 무효여도 200 이다(멱등).
            이미 발급된 액세스 토큰은 만료 시각까지 유효하므로 프론트도 보관 중인 값을 지워야 한다.
            """)
    @PostMapping("/logout")
    public ApiResponse<Void> logout(@RequestBody(required = false) RefreshTokenRequest request,
                                    HttpServletRequest httpRequest,
                                    HttpServletResponse response) {
        authService.logout(presentedToken(request, httpRequest));
        response.addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookies.clear());
        return ApiResponse.ok("로그아웃되었습니다.");
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

    @Operation(summary = "비밀번호 재설정", description = "성공하면 그 사용자의 모든 리프레시 토큰(다른 기기 세션 포함)을 폐기한다.")
    @PostMapping("/password/reset")
    public ApiResponse<Void> resetPassword(@Valid @RequestBody PasswordResetRequest request) {
        authService.resetPassword(request);
        return ApiResponse.ok("비밀번호가 변경되었습니다.");
    }

    /** refresh 실패는 공통 401 포맷에 더해 브라우저 쿠키도 지운다. 남겨두면 매번 같은 실패를 반복한다 */
    @ExceptionHandler(InvalidRefreshTokenException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidRefreshToken(InvalidRefreshTokenException e) {
        return ResponseEntity.status(e.getErrorCode().getStatus())
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookies.clear())
                .body(ApiResponse.error(e.getErrorCode()));
    }

    /** 쿠키가 우선이다. 웹이 본문으로 토큰을 보낼 일은 없고, 앱은 쿠키가 없다 */
    private String presentedToken(RefreshTokenRequest request, HttpServletRequest httpRequest) {
        return refreshTokenCookies.read(httpRequest)
                .orElse(request == null ? null : request.refreshToken());
    }

    /** 쿠키는 항상 심고, 본문에는 앱일 때만 싣는다 */
    private LoginResponse withSession(AuthResult result, String platform, HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookies.issue(result.refreshToken()));
        if (APP_PLATFORM.equalsIgnoreCase(platform)) {
            return result.response().withRefreshToken(result.refreshToken());
        }
        return result.response();
    }
}

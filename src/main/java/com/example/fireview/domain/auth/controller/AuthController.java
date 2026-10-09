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

import java.util.Optional;

@Tag(name = "인증", description = "회원가입·로그인·세션 연장·로그아웃·비밀번호 재설정")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    /**
     * 앱은 쿠키 저장소가 없어서 로그인·회원가입 때 이 헤더로 리프레시 토큰을 본문으로 받겠다고 알린다.
     * 헤더는 웹 스크립트도 붙일 수 있으므로 이것만으로 앱이라고 믿지 않는다({@link #isNativeApp} 참고).
     */
    static final String CLIENT_PLATFORM_HEADER = "X-Client-Platform";
    static final String APP_PLATFORM = "app";
    /** 브라우저가 붙이는 Fetch Metadata. Origin 과 함께 페이지 스크립트가 지우거나 바꿀 수 없다 */
    static final String SEC_FETCH_SITE_HEADER = "Sec-Fetch-Site";
    static final String SEC_FETCH_MODE_HEADER = "Sec-Fetch-Mode";

    private final AuthService authService;
    private final RefreshTokenCookies refreshTokenCookies;

    @Operation(summary = "회원가입", description = """
            성공하면 리프레시 토큰을 HttpOnly 쿠키(Set-Cookie)로 내려준다.
            X-Client-Platform: app 이고 브라우저 요청이 아니면(Origin·Sec-Fetch-* 없음) 쿠키 대신
            본문 data.refreshToken 으로 준다. 브라우저 요청이면 헤더가 있어도 쿠키로만 준다.
            리프레시 저장소 장애 시에는 refreshToken 없이(쿠키·본문 모두 없음) 로그인만 성공한다.
            """)
    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<LoginResponse> signup(@Valid @RequestBody SignupRequest request,
                                             @Parameter(description = "앱이면 app — 본문으로 refreshToken 을 받는다") @RequestHeader(value = CLIENT_PLATFORM_HEADER, required = false) String platform,
                                             HttpServletRequest httpRequest,
                                             HttpServletResponse response) {
        LoginResponse body = respondToLogin(authService.signup(request), platform, httpRequest, response);
        return ApiResponse.success("회원가입이 완료되었습니다.", body);
    }

    @Operation(summary = "로그인", description = """
            성공하면 리프레시 토큰을 HttpOnly 쿠키(Set-Cookie)로 내려준다.
            X-Client-Platform: app 이고 브라우저 요청이 아니면(Origin·Sec-Fetch-* 없음) 쿠키 대신
            본문 data.refreshToken 으로 준다. 브라우저 요청이면 헤더가 있어도 쿠키로만 준다.
            리프레시 저장소 장애 시에는 refreshToken 없이(쿠키·본문 모두 없음) 로그인만 성공한다.
            """)
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request,
                                            @Parameter(description = "앱이면 app — 본문으로 refreshToken 을 받는다") @RequestHeader(value = CLIENT_PLATFORM_HEADER, required = false) String platform,
                                            HttpServletRequest httpRequest,
                                            HttpServletResponse response) {
        return ApiResponse.success(respondToLogin(authService.login(request), platform, httpRequest, response));
    }

    @Operation(summary = "세션 연장(액세스 토큰 재발급)", description = """
            리프레시 토큰을 검증해 새 액세스 토큰을 발급하고 리프레시 토큰을 회전한다. 응답 본문은 로그인과 같은 모양이다.
            새 값은 받은 경로로만 돌려준다 — 쿠키 review_rt 로 왔으면 Set-Cookie 로만(본문 refreshToken 없음,
            X-Client-Platform 무시), 쿠키 없이 본문 refreshToken(앱)으로 왔으면 본문으로만(Set-Cookie 없음).
            30분 동안 refresh 가 없으면 만료된다. 실패하면 401 REFRESH_TOKEN_INVALID 와 함께 쿠키를 지운다 —
            프론트는 재시도하지 말고 로그아웃 처리할 것. 여러 탭이 동시에 불러도 15초 안이면 모두 성공한다.
            리프레시 저장소 장애면 503 AUTH_SESSION_UNAVAILABLE(쿠키 유지) — 로그아웃시키지 말고 나중에 다시 시도할 것.
            """)
    @PostMapping("/refresh")
    public ApiResponse<LoginResponse> refresh(@RequestBody(required = false) RefreshTokenRequest request,
                                              HttpServletRequest httpRequest,
                                              HttpServletResponse response) {
        // 쿠키로 인증한 요청에 본문 토큰을 주면, 웹 XSS 가 앱 헤더만 붙여 HttpOnly 값을 읽어 간다.
        // 그래서 응답 경로는 클라이언트가 고르는 헤더가 아니라 토큰이 들어온 경로로 정한다
        Optional<String> cookieToken = refreshTokenCookies.read(httpRequest);
        if (cookieToken.isPresent()) {
            return ApiResponse.success(respondWithCookie(authService.refresh(cookieToken.get()), response));
        }
        String bodyToken = request == null ? null : request.refreshToken();
        return ApiResponse.success(respondInBody(authService.refresh(bodyToken)));
    }

    @Operation(summary = "로그아웃", description = """
            리프레시 토큰을 폐기하고 쿠키를 지운다. 토큰이 없거나 이미 무효여도 200 이다(멱등).
            이미 발급된 액세스 토큰은 만료 시각까지 유효하므로 프론트도 보관 중인 값을 지워야 한다.
            리프레시 저장소 장애면 폐기를 확인할 수 없어 503 AUTH_SESSION_UNAVAILABLE(쿠키 유지)로 실패한다.
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

    /** 로그인·회원가입: 네이티브 앱이면 본문으로만, 그 밖에는 쿠키로만 준다 */
    private LoginResponse respondToLogin(AuthResult result, String platform,
                                         HttpServletRequest httpRequest, HttpServletResponse response) {
        if (isNativeApp(platform, httpRequest)) {
            return respondInBody(result);
        }
        return respondWithCookie(result, response);
    }

    /** 리프레시 토큰은 쿠키로만 준다. 본문에는 절대 싣지 않는다 */
    private LoginResponse respondWithCookie(AuthResult result, HttpServletResponse response) {
        if (result.hasRefreshToken()) {
            response.addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookies.issue(result.refreshToken()));
        }
        return result.response();
    }

    /** 앱 전용. 본문으로만 준다. 앱은 쿠키를 쓰지 않으므로 Set-Cookie 는 내리지 않는다 */
    private LoginResponse respondInBody(AuthResult result) {
        if (result.hasRefreshToken()) {
            return result.response().withRefreshToken(result.refreshToken());
        }
        return result.response();
    }

    /**
     * 앱 헤더가 있고 브라우저 흔적이 없을 때만 네이티브 앱으로 본다.
     *
     * <p>브라우저는 POST 에 Origin 을, 최신 브라우저는 Sec-Fetch-* 도 붙이고, 페이지 스크립트는
     * 이 헤더들을 지우거나 바꿀 수 없다. 그래서 re-view.kr 의 스크립트가 앱 헤더를 붙여도 여기서
     * 걸러져 쿠키 응답이 된다. 네이티브 앱(Flutter http 등)은 이 헤더를 보내지 않는다.
     */
    private boolean isNativeApp(String platform, HttpServletRequest httpRequest) {
        return APP_PLATFORM.equalsIgnoreCase(platform)
                && httpRequest.getHeader(HttpHeaders.ORIGIN) == null
                && httpRequest.getHeader(SEC_FETCH_SITE_HEADER) == null
                && httpRequest.getHeader(SEC_FETCH_MODE_HEADER) == null;
    }
}

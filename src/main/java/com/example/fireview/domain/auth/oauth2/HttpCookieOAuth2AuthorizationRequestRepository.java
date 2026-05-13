package com.example.fireview.domain.auth.oauth2;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.SerializationUtils;

import java.util.Arrays;
import java.util.Base64;
import java.util.Optional;

/**
 * OAuth2 authorization request를 HTTP 세션 대신 쿠키에 저장합니다.
 *
 * 기본 구현(HttpSessionOAuth2AuthorizationRequestRepository)은 state를 세션에 저장하는데,
 * 네이버/구글 콜백은 cross-site 리다이렉트이므로 SameSite 정책에 의해 세션 쿠키가 차단될 수 있습니다.
 * 쿠키 기반으로 전환하면 이 문제를 해결할 수 있습니다.
 */
@Component
public class HttpCookieOAuth2AuthorizationRequestRepository
        implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

    public static final String OAUTH2_AUTHORIZATION_REQUEST_COOKIE = "oauth2_auth_request";
    private static final int COOKIE_EXPIRE_SECONDS = 180; // 3분

    @Override
    public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
        return getCookie(request, OAUTH2_AUTHORIZATION_REQUEST_COOKIE)
                .map(cookie -> deserialize(cookie.getValue()))
                .orElse(null);
    }

    @Override
    public void saveAuthorizationRequest(OAuth2AuthorizationRequest authorizationRequest,
                                         HttpServletRequest request,
                                         HttpServletResponse response) {
        if (authorizationRequest == null) {
            deleteCookie(request, response, OAUTH2_AUTHORIZATION_REQUEST_COOKIE);
            return;
        }
        addCookie(response, OAUTH2_AUTHORIZATION_REQUEST_COOKIE,
                serialize(authorizationRequest), COOKIE_EXPIRE_SECONDS);
    }

    @Override
    public OAuth2AuthorizationRequest removeAuthorizationRequest(HttpServletRequest request,
                                                                  HttpServletResponse response) {
        OAuth2AuthorizationRequest authRequest = loadAuthorizationRequest(request);
        deleteCookie(request, response, OAUTH2_AUTHORIZATION_REQUEST_COOKIE);
        return authRequest;
    }

    // ── 쿠키 유틸 ────────────────────────────────────────────────────────────

    private static Optional<Cookie> getCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return Optional.empty();
        return Arrays.stream(cookies)
                .filter(c -> c.getName().equals(name))
                .findFirst();
    }

    private static void addCookie(HttpServletResponse response, String name,
                                   String value, int maxAge) {
        Cookie cookie = new Cookie(name, value);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setSecure(true);       // HTTPS 전용
        cookie.setMaxAge(maxAge);
        // SameSite=None 설정 (cross-site 리다이렉트에서 쿠키 전송 허용)
        response.addHeader("Set-Cookie",
                String.format("%s=%s; Path=/; Max-Age=%d; HttpOnly; Secure; SameSite=None",
                        name, value, maxAge));
    }

    private static void deleteCookie(HttpServletRequest request,
                                      HttpServletResponse response, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return;
        Arrays.stream(cookies)
                .filter(c -> c.getName().equals(name))
                .forEach(c -> {
                    response.addHeader("Set-Cookie",
                            String.format("%s=; Path=/; Max-Age=0; HttpOnly; Secure; SameSite=None",
                                    name));
                });
    }

    @SuppressWarnings("deprecation")
    private static String serialize(OAuth2AuthorizationRequest request) {
        return Base64.getUrlEncoder()
                .encodeToString(SerializationUtils.serialize(request));
    }

    @SuppressWarnings({"deprecation", "unchecked"})
    private static OAuth2AuthorizationRequest deserialize(String value) {
        return (OAuth2AuthorizationRequest) SerializationUtils.deserialize(
                Base64.getUrlDecoder().decode(value));
    }
}

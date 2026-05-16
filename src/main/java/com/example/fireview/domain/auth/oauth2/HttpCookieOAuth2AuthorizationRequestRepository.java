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
 * OAuth2 authorization request를 HTTP 세션 대신 쿠키에 저장한다.
 *
 * 세션 기반 구현(HttpSessionOAuth2AuthorizationRequestRepository)은 JSESSIONID에 의존한다.
 * JSESSIONID는 Tomcat 내부 finalization 경로로 설정되어 SameSite=None 적용이 불안정하다.
 * 쿠키에 직접 addHeader("Set-Cookie", ...) 로 저장하면 cross-site 콜백에서도 확실히 전달된다.
 */
@Component
public class HttpCookieOAuth2AuthorizationRequestRepository
        implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

    public static final String OAUTH2_AUTHORIZATION_REQUEST_COOKIE = "oauth2_auth_request";
    private static final int COOKIE_EXPIRE_SECONDS = 180;

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

    private static Optional<Cookie> getCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return Optional.empty();
        return Arrays.stream(cookies)
                .filter(c -> c.getName().equals(name))
                .findFirst();
    }

    private static void addCookie(HttpServletResponse response, String name,
                                   String value, int maxAge) {
        // response.addCookie() 대신 addHeader()로 직접 써서
        // SameSite=None 이 Tomcat/필터 레이어 우회 없이 확실히 포함되도록 한다
        response.addHeader("Set-Cookie",
                String.format("%s=%s; Path=/; Max-Age=%d; HttpOnly; Secure; SameSite=None",
                        name, value, maxAge));
    }

    private static void deleteCookie(HttpServletRequest request,
                                      HttpServletResponse response, String name) {
        if (request.getCookies() == null) return;
        Arrays.stream(request.getCookies())
                .filter(c -> c.getName().equals(name))
                .forEach(c ->
                        response.addHeader("Set-Cookie",
                                String.format("%s=; Path=/; Max-Age=0; HttpOnly; Secure; SameSite=None",
                                        name)));
    }

    @SuppressWarnings("deprecation")
    private static String serialize(OAuth2AuthorizationRequest request) {
        return Base64.getUrlEncoder()
                .encodeToString(SerializationUtils.serialize(request));
    }

    @SuppressWarnings({"deprecation", "unchecked"})
    private static OAuth2AuthorizationRequest deserialize(String value) {
        try {
            return (OAuth2AuthorizationRequest) SerializationUtils.deserialize(
                    Base64.getUrlDecoder().decode(value));
        } catch (Exception e) {
            return null;
        }
    }
}

package com.example.fireview.domain.auth.cookie;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;

/**
 * 리프레시 토큰 쿠키를 만들고 읽는다.
 *
 * <p>기본값: {@code review_rt; Path=/api/auth; HttpOnly; Secure; SameSite=Lax; Max-Age=TTL}.
 * Domain 은 붙이지 않는다. 웹은 re-view.kr 의 Vercel rewrite(/api/*, /login/oauth2/*) 를 거쳐
 * 백엔드에 닿으므로, 응답의 Set-Cookie 는 브라우저 입장에서 re-view.kr 이 내려준 첫 출처
 * 호스트 전용 쿠키가 된다. 그래서 api.re-view.kr 이나 다른 서브도메인으로는 새지 않는다.
 *
 * <p><b>CSRF</b>: 쿠키만으로 인증하는 엔드포인트는 POST /api/auth/refresh·logout 둘뿐이다.
 * SameSite=Lax 는 다른 사이트에서 시작된 POST(폼·fetch)에 쿠키를 싣지 않으므로 이것으로 막는다.
 * 같은 사이트 안에서 위조되더라도 refresh 의 결과(새 액세스 토큰)는 CORS 를 통과한 출처만 읽을 수
 * 있고, 쿠키는 브라우저가 알아서 새 값으로 갈아끼우므로 공격자가 얻는 것이 없다. logout 위조는
 * 강제 로그아웃뿐이다. 그래서 Origin/Referer 검사는 더하지 않았다.
 */
@Component
public class RefreshTokenCookies {

    private final String name;
    private final String path;
    private final boolean secure;
    private final String sameSite;
    private final Duration maxAge;

    public RefreshTokenCookies(@Value("${app.auth.refresh.cookie.name:review_rt}") String name,
                               @Value("${app.auth.refresh.cookie.path:/api/auth}") String path,
                               @Value("${app.auth.refresh.cookie.secure:true}") boolean secure,
                               @Value("${app.auth.refresh.cookie.same-site:Lax}") String sameSite,
                               @Value("${app.auth.refresh.ttl:PT30M}") Duration maxAge) {
        this.name = name;
        this.path = path;
        this.secure = secure;
        this.sameSite = sameSite;
        this.maxAge = maxAge;
    }

    /** Set-Cookie 헤더 값. 토큰 원문이 들어 있으니 로그로 남기지 않는다 */
    public String issue(String token) {
        return base(token).maxAge(maxAge).build().toString();
    }

    /** 브라우저에서 쿠키를 지우는 Set-Cookie 헤더 값(Max-Age=0) */
    public String clear() {
        return base("").maxAge(Duration.ZERO).build().toString();
    }

    public Optional<String> read(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }
        return Arrays.stream(cookies)
                .filter(cookie -> name.equals(cookie.getName()))
                .map(Cookie::getValue)
                .filter(value -> !value.isBlank())
                .findFirst();
    }

    public String getName() {
        return name;
    }

    private ResponseCookie.ResponseCookieBuilder base(String value) {
        return ResponseCookie.from(name, value)
                .path(path)
                .httpOnly(true)
                .secure(secure)
                .sameSite(sameSite);
    }
}

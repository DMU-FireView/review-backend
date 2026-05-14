package com.example.fireview.global.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 모든 Set-Cookie 헤더에 SameSite=None; Secure 강제 적용 필터
 *
 * TomcatConfig의 Rfc6265CookieProcessor만으로 부족한 경우를 대비한
 * 추가 안전장치. addCookie() 호출을 가로채어 SameSite=None; Secure를 붙임.
 *
 * OAuth2 콜백(cross-site 리다이렉트) 시 JSESSIONID가 전달되려면
 * 반드시 SameSite=None; Secure 속성이 있어야 함.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SameSiteCookieFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        if (response instanceof HttpServletResponse httpResponse) {
            chain.doFilter(request, new SameSiteResponseWrapper(httpResponse));
        } else {
            chain.doFilter(request, response);
        }
    }

    static class SameSiteResponseWrapper extends HttpServletResponseWrapper {

        public SameSiteResponseWrapper(HttpServletResponse response) {
            super(response);
        }

        @Override
        public void addCookie(Cookie cookie) {
            // 기존 addCookie() 대신 Set-Cookie 헤더를 직접 작성하여
            // SameSite=None; Secure 속성을 확실히 포함시킴
            String cookieHeader = buildSameSiteNoneCookieHeader(cookie);
            super.addHeader("Set-Cookie", cookieHeader);
        }

        private String buildSameSiteNoneCookieHeader(Cookie cookie) {
            StringBuilder sb = new StringBuilder();
            sb.append(cookie.getName()).append("=").append(cookie.getValue());

            String path = cookie.getPath();
            sb.append("; Path=").append(path != null ? path : "/");

            if (cookie.getMaxAge() >= 0) {
                sb.append("; Max-Age=").append(cookie.getMaxAge());
            }

            if (cookie.getDomain() != null) {
                sb.append("; Domain=").append(cookie.getDomain());
            }

            if (cookie.isHttpOnly()) {
                sb.append("; HttpOnly");
            }

            // SameSite=None은 Secure와 함께 사용해야 유효
            sb.append("; Secure");
            sb.append("; SameSite=None");

            return sb.toString();
        }
    }
}

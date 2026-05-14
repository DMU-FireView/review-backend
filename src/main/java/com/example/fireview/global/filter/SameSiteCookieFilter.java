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
 * Set-Cookie 헤더 전체에 SameSite=None; Secure 보완 필터
 *
 * TomcatConfig(Rfc6265CookieProcessor)가 JSESSIONID를 담당하고,
 * 이 필터는 addCookie() / addHeader("Set-Cookie") 경로로 오는
 * 나머지 쿠키들에 SameSite=None; Secure 를 보완합니다.
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

        /** addCookie() 경유 쿠키 */
        @Override
        public void addCookie(Cookie cookie) {
            super.addHeader("Set-Cookie", buildCookieHeader(cookie));
        }

        /** addHeader("Set-Cookie") 경유 쿠키 (서블릿 레벨) */
        @Override
        public void addHeader(String name, String value) {
            if ("Set-Cookie".equalsIgnoreCase(name)) {
                value = ensureSameSiteNone(value);
            }
            super.addHeader(name, value);
        }

        /** setHeader("Set-Cookie") 경유 쿠키 */
        @Override
        public void setHeader(String name, String value) {
            if ("Set-Cookie".equalsIgnoreCase(name)) {
                value = ensureSameSiteNone(value);
            }
            super.setHeader(name, value);
        }

        private String ensureSameSiteNone(String header) {
            String lower = header.toLowerCase();
            if (!lower.contains("samesite")) header += "; SameSite=None";
            if (!lower.contains("secure"))   header += "; Secure";
            return header;
        }

        private String buildCookieHeader(Cookie cookie) {
            StringBuilder sb = new StringBuilder();
            sb.append(cookie.getName()).append("=").append(cookie.getValue());
            sb.append("; Path=").append(cookie.getPath() != null ? cookie.getPath() : "/");
            if (cookie.getMaxAge() >= 0) sb.append("; Max-Age=").append(cookie.getMaxAge());
            if (cookie.getDomain() != null) sb.append("; Domain=").append(cookie.getDomain());
            if (cookie.isHttpOnly()) sb.append("; HttpOnly");
            sb.append("; Secure; SameSite=None");
            return sb.toString();
        }
    }
}

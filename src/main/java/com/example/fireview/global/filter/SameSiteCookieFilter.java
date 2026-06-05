package com.example.fireview.global.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 모든 Set-Cookie 헤더에 SameSite=None; Secure 강제 적용 필터
 *
 * [핵심 수정]
 * Tomcat은 JSESSIONID를 response.addCookie()가 아닌
 * 내부적으로 addHeader("Set-Cookie", ...) 로 직접 씁니다.
 * 따라서 addCookie() 만 가로채면 JSESSIONID에는 적용이 안 됩니다.
 *
 * addHeader("Set-Cookie", ...) 와 setHeader("Set-Cookie", ...) 를
 * 모두 가로채어 SameSite=None; Secure 를 확실히 추가합니다.
 */
@Slf4j
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

        /**
         * response.addCookie() 경유 쿠키 (일반 쿠키)
         */
        @Override
        public void addCookie(Cookie cookie) {
            String header = buildCookieHeader(cookie);
            log.debug("[SameSiteFilter] addCookie intercepted: {}", header);
            super.addHeader("Set-Cookie", header);
        }

        /**
         * addHeader("Set-Cookie", ...) 경유 쿠키 (JSESSIONID 포함)
         * Tomcat이 JSESSIONID를 여기로 씁니다.
         */
        @Override
        public void addHeader(String name, String value) {
            if ("Set-Cookie".equalsIgnoreCase(name)) {
                value = ensureSameSiteNone(value);
                log.debug("[SameSiteFilter] addHeader Set-Cookie: {}", value);
            }
            super.addHeader(name, value);
        }

        /**
         * setHeader("Set-Cookie", ...) 경유 쿠키
         */
        @Override
        public void setHeader(String name, String value) {
            if ("Set-Cookie".equalsIgnoreCase(name)) {
                value = ensureSameSiteNone(value);
                log.debug("[SameSiteFilter] setHeader Set-Cookie: {}", value);
            }
            super.setHeader(name, value);
        }

        /**
         * Set-Cookie 헤더에 SameSite=None; Secure 가 없으면 추가
         */
        private String ensureSameSiteNone(String cookieHeader) {
            String lower = cookieHeader.toLowerCase();
            if (!lower.contains("samesite")) {
                cookieHeader += "; SameSite=None";
            }
            if (!lower.contains("secure")) {
                cookieHeader += "; Secure";
            }
            return cookieHeader;
        }

        private String buildCookieHeader(Cookie cookie) {
            StringBuilder sb = new StringBuilder();
            sb.append(cookie.getName()).append("=").append(cookie.getValue());
            sb.append("; Path=").append(cookie.getPath() != null ? cookie.getPath() : "/");
            if (cookie.getMaxAge() >= 0) sb.append("; Max-Age=").append(cookie.getMaxAge());
            if (cookie.getDomain() != null) sb.append("; Domain=").append(cookie.getDomain());
            if (cookie.isHttpOnly()) sb.append("; HttpOnly");
            sb.append("; Secure");
            sb.append("; SameSite=None");
            return sb.toString();
        }
    }
}

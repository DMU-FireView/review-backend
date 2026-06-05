package com.example.fireview.global.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collection;

/**
 * 응답의 Set-Cookie 헤더를 로그로 출력하는 디버그 필터
 *
 * SameSite=None이 실제로 JSESSIONID에 적용됐는지 서버 로그에서 확인용.
 * 원인 파악 후 제거 가능.
 */
@Slf4j
@Component
@Order(Integer.MIN_VALUE + 1)
public class SetCookieLoggingFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        chain.doFilter(request, response);

        // 응답 처리 후 Set-Cookie 헤더 로그 출력
        if (request instanceof HttpServletRequest httpRequest
                && response instanceof HttpServletResponse httpResponse) {

            String path = httpRequest.getRequestURI();
            Collection<String> setCookieHeaders = httpResponse.getHeaders("Set-Cookie");

            if (!setCookieHeaders.isEmpty()) {
                log.info("[SetCookieLog] path={}, Set-Cookie 개수={}", path, setCookieHeaders.size());
                setCookieHeaders.forEach(cookie ->
                        log.info("[SetCookieLog] Set-Cookie: {}", cookie));
            }
        }
    }
}

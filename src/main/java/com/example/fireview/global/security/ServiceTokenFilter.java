package com.example.fireview.global.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

/**
 * 서버 간 호출(Data 서버 → Spring)을 인증하는 필터.
 *
 * /api/internal/** 경로에 한해 X-Service-Token 헤더를 검사하고,
 * 설정된 토큰과 일치하면 ROLE_SERVICE 권한을 가진 인증 객체를 SecurityContext에 넣는다.
 * SecurityConfig 에서 /api/internal/** 은 ROLE_SERVICE 를 요구하므로,
 * 이 필터가 인증을 세팅하지 않으면 요청은 401로 거부된다 (fail-closed).
 *
 * 토큰이 설정되어 있지 않으면(빈 문자열) 어떤 요청도 인증하지 않는다.
 */
@Slf4j
@Component
public class ServiceTokenFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Service-Token";
    public static final String INTERNAL_PATH_PREFIX = "/api/internal/";
    public static final String ROLE_SERVICE = "ROLE_SERVICE";
    private static final String SERVICE_PRINCIPAL = "data-server";

    private final byte[] expectedToken;

    public ServiceTokenFilter(@Value("${app.service-token:}") String serviceToken) {
        this.expectedToken = serviceToken == null
                ? new byte[0]
                : serviceToken.getBytes(StandardCharsets.UTF_8);
        if (expectedToken.length == 0) {
            log.warn("[ServiceToken] app.service-token 이 비어 있어 /api/internal/** 요청을 모두 거부한다");
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith(INTERNAL_PATH_PREFIX);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String provided = request.getHeader(HEADER);

        if (isValid(provided)) {
            var authentication = new UsernamePasswordAuthenticationToken(
                    SERVICE_PRINCIPAL, null, List.of(new SimpleGrantedAuthority(ROLE_SERVICE)));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } else {
            log.warn("[ServiceToken] 인증 실패 - uri={}, remote={}", request.getRequestURI(), request.getRemoteAddr());
        }

        filterChain.doFilter(request, response);
    }

    /** 타이밍 공격을 피하기 위해 길이가 달라도 상수 시간에 비교한다. */
    private boolean isValid(String provided) {
        if (expectedToken.length == 0 || provided == null || provided.isBlank()) {
            return false;
        }
        byte[] providedBytes = provided.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(expectedToken, providedBytes);
    }
}

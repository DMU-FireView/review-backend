package com.example.fireview.global.config;

import com.example.fireview.domain.auth.oauth2.CustomOAuth2UserService;
import com.example.fireview.domain.auth.oauth2.OAuth2SuccessHandler;
import com.example.fireview.global.security.CustomAuthenticationEntryPoint;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtDecoder jwtDecoder;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;
    private final CustomAuthenticationEntryPoint authenticationEntryPoint;
    private final List<String> allowedOriginPatterns;

    public SecurityConfig(JwtDecoder jwtDecoder,
                          CustomOAuth2UserService customOAuth2UserService,
                          OAuth2SuccessHandler oAuth2SuccessHandler,
                          CustomAuthenticationEntryPoint authenticationEntryPoint,
                          @Value("${app.cors.allowed-origins}") List<String> allowedOriginPatterns) {
        this.jwtDecoder = jwtDecoder;
        this.customOAuth2UserService = customOAuth2UserService;
        this.oAuth2SuccessHandler = oAuth2SuccessHandler;
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.allowedOriginPatterns = allowedOriginPatterns;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // OAuth2 state는 세션에 저장.
                // 세션 쿠키는 TomcatConfig + SameSiteCookieFilter로 SameSite=None; Secure 적용
                // → 네이버/구글 cross-site 콜백에서도 JSESSIONID가 브라우저에 의해 차단되지 않음
                // sessionFixation().newSession(): 인증 성공 후 새 세션 발급 → 세션 고정 공격 방지
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                        .sessionFixation().newSession()
                        .invalidSessionUrl("/oauth2/authorization/naver"))
                .headers(headers ->
                        headers.frameOptions(frame -> frame.sameOrigin()))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/landing/**").permitAll()
                        .requestMatchers("/api/products/**").permitAll()
                        .requestMatchers("/api/dashboard/**").permitAll()
                        .requestMatchers("/api/search/**").permitAll()
                        .requestMatchers("/api/keywords/**").permitAll()
                        .requestMatchers("/api/analysis/**").permitAll()
                        .requestMatchers("/h2-console/**").permitAll()
                        .requestMatchers("/actuator/**").permitAll()
                        .requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll()
                        .requestMatchers("/api/reviews/*/feedback").authenticated()
                        .requestMatchers("/api/wishlist/**").authenticated()
                        .requestMatchers("/api/cart/**").authenticated()
                        .anyRequest().authenticated())
                .exceptionHandling(ex ->
                        ex.authenticationEntryPoint(authenticationEntryPoint))
                .oauth2ResourceServer(oauth2 ->
                        oauth2.jwt(jwt -> jwt.decoder(jwtDecoder)))
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo ->
                                userInfo.userService(customOAuth2UserService))
                        .successHandler(oAuth2SuccessHandler)
                        .failureHandler((request, response, exception) -> {
                            org.slf4j.LoggerFactory.getLogger(SecurityConfig.class)
                                    .error("[OAuth2] 로그인 실패 - {}: {}", exception.getClass().getSimpleName(), exception.getMessage());

                            // [DEBUG] 리다이렉트 대신 HTML 에러 페이지 직접 응답
                            // 원인 파악 후 아래 블록을 제거하고 sendRedirect로 복구
                            String cookieHeader = request.getHeader("Cookie");
                            String sessionId = request.getRequestedSessionId();
                            boolean sessionValid = request.isRequestedSessionIdValid();
                            boolean sessionIdFromCookie = request.isRequestedSessionIdFromCookie();

                            // 현재 세션 생성 여부 확인
                            jakarta.servlet.http.HttpSession existingSession = request.getSession(false);
                            String currentSessionId = existingSession != null ? existingSession.getId() : "(세션 없음)";
                            boolean sessionIdsMatch = existingSession != null && existingSession.getId().equals(sessionId);

                            response.setContentType("text/html; charset=UTF-8");
                            response.setStatus(200);
                            response.getWriter().write("""
                                <!DOCTYPE html>
                                <html>
                                <head><meta charset="UTF-8"><title>OAuth2 디버그</title></head>
                                <body style="font-family:monospace; padding:20px; background:#1a1a1a; color:#00ff00;">
                                <h2 style="color:#ff4444;">❌ OAuth2 로그인 실패</h2>
                                <table border="1" style="border-collapse:collapse; color:#fff; width:100%%;">
                                  <tr><td style="padding:8px;background:#333;">에러 타입</td><td style="padding:8px;">%s</td></tr>
                                  <tr><td style="padding:8px;background:#333;">에러 메시지</td><td style="padding:8px;">%s</td></tr>
                                  <tr><td style="padding:8px;background:#333;">Cookie 헤더</td><td style="padding:8px;">%s</td></tr>
                                  <tr><td style="padding:8px;background:#333;">브라우저가 보낸 세션 ID</td><td style="padding:8px;">%s</td></tr>
                                  <tr><td style="padding:8px;background:#333;">세션 유효</td><td style="padding:8px;color:%s;">%s</td></tr>
                                  <tr><td style="padding:8px;background:#333;">쿠키로 전달됨</td><td style="padding:8px;">%s</td></tr>
                                  <tr><td style="padding:8px;background:#333;">서버 현재 세션 ID</td><td style="padding:8px;">%s</td></tr>
                                  <tr><td style="padding:8px;background:#333;">세션 ID 일치</td><td style="padding:8px;color:%s;">%s</td></tr>
                                  <tr><td style="padding:8px;background:#333;">요청 URL</td><td style="padding:8px;font-size:11px;">%s</td></tr>
                                </table>
                                <p style="color:#aaa;margin-top:16px;">
                                  ✅ 세션 유효=true + 세션ID 일치=true 이면 SameSite 문제 해결됨<br>
                                  ❌ 세션 유효=false 이면 SameSite=None이 아직 미적용
                                </p>
                                </body></html>
                                """.formatted(
                                    exception.getClass().getSimpleName(),
                                    exception.getMessage(),
                                    cookieHeader != null ? cookieHeader : "(없음)",
                                    sessionId != null ? sessionId : "(없음)",
                                    sessionValid ? "#00ff00" : "#ff4444",
                                    sessionValid,
                                    sessionIdFromCookie,
                                    currentSessionId,
                                    sessionIdsMatch ? "#00ff00" : "#ff4444",
                                    sessionIdsMatch,
                                    request.getRequestURL() + (request.getQueryString() != null ? "?" + request.getQueryString() : "")
                            ));
                        }));

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(allowedOriginPatterns);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("Authorization"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}

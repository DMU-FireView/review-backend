package com.example.fireview.global.config;

import com.example.fireview.domain.auth.oauth2.CustomOAuth2UserService;
import com.example.fireview.domain.auth.oauth2.OAuth2SuccessHandler;
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
    private final List<String> allowedOriginPatterns;

    public SecurityConfig(JwtDecoder jwtDecoder,
                          CustomOAuth2UserService customOAuth2UserService,
                          OAuth2SuccessHandler oAuth2SuccessHandler,
                          @Value("${app.cors.allowed-origins}") List<String> allowedOriginPatterns) {
        this.jwtDecoder = jwtDecoder;
        this.customOAuth2UserService = customOAuth2UserService;
        this.oAuth2SuccessHandler = oAuth2SuccessHandler;
        this.allowedOriginPatterns = allowedOriginPatterns;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // OAuth2 인가 코드 흐름 중 state 저장을 위해 세션 허용 (IF_REQUIRED)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .headers(headers ->
                        headers.frameOptions(frame -> frame.sameOrigin()))
                .authorizeHttpRequests(auth -> auth
                        // ── 인증 불필요 (게스트/비회원 허용) ──────────────────────────
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/landing/**").permitAll()
                        // 상품 목록/상세/리뷰 조회 - 비회원도 열람 가능
                        .requestMatchers("/api/products/**").permitAll()
                        // 홈 대시보드 - 비회원은 개인화 없는 공개 버전 반환
                        .requestMatchers("/api/dashboard/**").permitAll()
                        // 네이버 쇼핑 검색 - 비회원도 검색 가능
                        .requestMatchers("/api/search/**").permitAll()
                        .requestMatchers("/api/keywords/**").permitAll()
                        .requestMatchers("/api/analysis/**").permitAll()
                        .requestMatchers("/h2-console/**").permitAll()
                        .requestMatchers("/actuator/**").permitAll()
                        // OAuth2 로그인 진입 경로 허용
                        .requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll()
                        // 찜 여부 확인은 비로그인도 접근 허용 (컨트롤러에서 401 포맷 반환)
                        .requestMatchers("/api/wishlist/*/check").permitAll()
                        // ── 인증 필요 ─────────────────────────────────────────────────
                        .requestMatchers("/api/reviews/*/feedback").authenticated()
                        .requestMatchers("/api/wishlist/**").authenticated()
                        .requestMatchers("/api/cart/**").authenticated()
                        .anyRequest().authenticated())
                // JWT 기반 API 인증
                .oauth2ResourceServer(oauth2 ->
                        oauth2.jwt(jwt -> jwt.decoder(jwtDecoder)))
                // OAuth2 소셜 로그인
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo ->
                                userInfo.userService(customOAuth2UserService))
                        .successHandler(oAuth2SuccessHandler));

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        // setAllowedOrigins() 가 아닌 setAllowedOriginPatterns() 를 사용해
        // Flutter web dev server 처럼 동적 포트(http://localhost:*) 도 허용한다.
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

package com.example.fireview.global.config;

import com.example.fireview.domain.auth.oauth2.CustomOAuth2UserService;
import com.example.fireview.domain.auth.oauth2.HttpCookieOAuth2AuthorizationRequestRepository;
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
    private final HttpCookieOAuth2AuthorizationRequestRepository authorizationRequestRepository;
    private final List<String> allowedOriginPatterns;
    private final String frontendRedirectUri;

    public SecurityConfig(JwtDecoder jwtDecoder,
                          CustomOAuth2UserService customOAuth2UserService,
                          OAuth2SuccessHandler oAuth2SuccessHandler,
                          CustomAuthenticationEntryPoint authenticationEntryPoint,
                          HttpCookieOAuth2AuthorizationRequestRepository authorizationRequestRepository,
                          @Value("${app.cors.allowed-origins}") List<String> allowedOriginPatterns,
                          @Value("${oauth2.redirect-uri}") String frontendRedirectUri) {
        this.jwtDecoder = jwtDecoder;
        this.customOAuth2UserService = customOAuth2UserService;
        this.oAuth2SuccessHandler = oAuth2SuccessHandler;
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.authorizationRequestRepository = authorizationRequestRepository;
        this.allowedOriginPatterns = allowedOriginPatterns;
        this.frontendRedirectUri = frontendRedirectUri;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // OAuth2 state는 쿠키(HttpCookieOAuth2AuthorizationRequestRepository)에 저장
                // → JSESSIONID에 의존하지 않으므로 SameSite 세션 쿠키 문제 완전 우회
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
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
                        .requestMatchers("/api/reviews/feedbacks/me/**").authenticated()
                        .requestMatchers("/api/reports/**").authenticated()
                        .requestMatchers("/api/notifications/**").authenticated()
                        .requestMatchers("/api/users/me/**").authenticated()
                        .requestMatchers("/api/users/me").authenticated()
                        .requestMatchers("/api/wishlist/**").authenticated()
                        .requestMatchers("/api/cart/**").authenticated()
                        .anyRequest().authenticated())
                .exceptionHandling(ex ->
                        ex.authenticationEntryPoint(authenticationEntryPoint))
                .oauth2ResourceServer(oauth2 ->
                        oauth2.jwt(jwt -> jwt.decoder(jwtDecoder)))
                .oauth2Login(oauth2 -> oauth2
                        .authorizationEndpoint(auth ->
                                auth.authorizationRequestRepository(authorizationRequestRepository))
                        .userInfoEndpoint(userInfo ->
                                userInfo.userService(customOAuth2UserService))
                        .successHandler(oAuth2SuccessHandler)
                        .failureHandler((request, response, exception) -> {
                            org.slf4j.LoggerFactory.getLogger(SecurityConfig.class)
                                    .error("[OAuth2] 로그인 실패 - {}: {}", exception.getClass().getSimpleName(), exception.getMessage());
                            response.sendRedirect(frontendRedirectUri.replace("/oauth2/callback", "/login?error=oauth2"));
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

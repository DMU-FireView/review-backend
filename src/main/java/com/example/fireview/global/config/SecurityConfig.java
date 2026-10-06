package com.example.fireview.global.config;

import com.example.fireview.domain.auth.oauth2.CustomOAuth2UserService;
import com.example.fireview.domain.auth.oauth2.HttpCookieOAuth2AuthorizationRequestRepository;
import com.example.fireview.domain.auth.oauth2.OAuth2SuccessHandler;
import com.example.fireview.global.security.CustomAuthenticationEntryPoint;
import com.example.fireview.global.security.ServiceTokenFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
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
    private final ServiceTokenFilter serviceTokenFilter;
    private final List<String> allowedOriginPatterns;
    private final String frontendRedirectUri;

    public SecurityConfig(JwtDecoder jwtDecoder,
                          CustomOAuth2UserService customOAuth2UserService,
                          OAuth2SuccessHandler oAuth2SuccessHandler,
                          CustomAuthenticationEntryPoint authenticationEntryPoint,
                          HttpCookieOAuth2AuthorizationRequestRepository authorizationRequestRepository,
                          ServiceTokenFilter serviceTokenFilter,
                          @Value("${app.cors.allowed-origins}") List<String> allowedOriginPatterns,
                          @Value("${oauth2.redirect-uri}") String frontendRedirectUri) {
        this.jwtDecoder = jwtDecoder;
        this.customOAuth2UserService = customOAuth2UserService;
        this.oAuth2SuccessHandler = oAuth2SuccessHandler;
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.authorizationRequestRepository = authorizationRequestRepository;
        this.serviceTokenFilter = serviceTokenFilter;
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
                        // 번호표 발급은 DB 에 행을 만든다. 비로그인에 열어두면 빈 행이 양산된다.
                        // 더 좁은 규칙이 먼저 와야 아래 permitAll 에 먹히지 않는다.
                        .requestMatchers(HttpMethod.POST, "/api/v2/products/**").authenticated()
                        // Data 서버 기반 상품 조회(v2). 기존 /api/products 와 같은 공개 범위
                        .requestMatchers("/api/v2/products/**").permitAll()
                        .requestMatchers("/api/dashboard/**").permitAll()
                        .requestMatchers("/api/search/**").permitAll()
                        .requestMatchers("/api/keywords/**").permitAll()
                        .requestMatchers("/api/analysis/**").permitAll()
                        .requestMatchers("/h2-console/**").permitAll()
                        .requestMatchers("/actuator/**").permitAll()
                        // API 문서 (springdoc). 노출을 막으려면 springdoc.api-docs.enabled=false
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll()
                        .requestMatchers("/api/reviews/*/feedback").authenticated()
                        .requestMatchers("/api/reviews/feedbacks/me/**").authenticated()
                        .requestMatchers("/api/reports/**").authenticated()
                        .requestMatchers("/api/notifications/**").authenticated()
                        .requestMatchers("/api/users/me/**").authenticated()
                        .requestMatchers("/api/users/me").authenticated()
                        .requestMatchers("/api/wishlist/**").authenticated()
                        .requestMatchers("/api/cart/**").authenticated()
                        .requestMatchers("/api/analysis-feedbacks/**").authenticated()
                        .requestMatchers("/api/feedback/**").authenticated()
                        .requestMatchers("/api/chat/**").authenticated()
                        .requestMatchers("/api/admin/**").hasAuthority("ROLE_ADMIN")
                        // 서버 간 호출(Data 서버 웹훅 등) — ServiceTokenFilter 가 부여하는 권한만 허용
                        .requestMatchers("/api/internal/**").hasAuthority(ServiceTokenFilter.ROLE_SERVICE)
                        .anyRequest().authenticated())
                .addFilterBefore(serviceTokenFilter, UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(ex ->
                        ex.authenticationEntryPoint(authenticationEntryPoint))
                // 만료·위조 토큰은 BearerTokenAuthenticationFilter 가 직접 진입점을 부른다.
                // exceptionHandling 의 진입점은 이 경로에 쓰이지 않으므로 여기에도 지정한다
                .oauth2ResourceServer(oauth2 -> oauth2
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .jwt(jwt -> jwt
                                .decoder(jwtDecoder)
                                .jwtAuthenticationConverter(jwtAuthenticationConverter())))
                .oauth2Login(oauth2 -> oauth2
                        .authorizationEndpoint(auth ->
                                auth.authorizationRequestRepository(authorizationRequestRepository))
                        .userInfoEndpoint(userInfo ->
                                userInfo.userService(customOAuth2UserService))
                        .successHandler(oAuth2SuccessHandler)
                        .failureHandler((request, response, exception) -> {
                            org.slf4j.LoggerFactory.getLogger(SecurityConfig.class)
                                    .error("[OAuth2] 로그인 실패 - {}: {}", exception.getClass().getSimpleName(), exception.getMessage());
                            // 프론트 OAuthCallbackPage가 queryParams['error']를 읽어 에러 처리
                            // /login?error=oauth2 대신 콜백 페이지로 error 파라미터 전달
                            String errorType = exception.getClass().getSimpleName()
                                    .contains("AccessDenied") ? "access_denied" : "server_error";
                            String failureUrl = org.springframework.web.util.UriComponentsBuilder
                                    .fromUriString(frontendRedirectUri)
                                    .queryParam("error", errorType)
                                    .build().toUriString();
                            response.sendRedirect(failureUrl);
                        }));

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            String role = jwt.getClaimAsString("role");
            if (role == null) return List.of();
            return List.<GrantedAuthority>of(new SimpleGrantedAuthority("ROLE_" + role));
        });
        return converter;
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

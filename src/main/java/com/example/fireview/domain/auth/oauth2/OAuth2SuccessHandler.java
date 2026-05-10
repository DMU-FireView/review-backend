package com.example.fireview.domain.auth.oauth2;

import com.example.fireview.domain.user.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtEncoder jwtEncoder;

    @Value("${jwt.expiration-ms}")
    private long jwtExpirationMs;

    @Value("${oauth2.redirect-uri}")
    private String frontendRedirectUri;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        OAuth2UserPrincipal principal = (OAuth2UserPrincipal) authentication.getPrincipal();
        User user = principal.getUser();

        String token = generateToken(user);

        // 프론트엔드로 JWT 토큰을 쿼리 파라미터에 담아 리다이렉트
        // 예: http://localhost:3000/oauth2/callback?token=eyJhbGci...&onboarding=false
        String redirectUrl = UriComponentsBuilder.fromUriString(frontendRedirectUri)
                .queryParam("token", token)
                .queryParam("onboarding", !user.isOnboardingCompleted())
                .build().toUriString();

        log.debug("OAuth2 로그인 성공 - email: {}, 온보딩 완료: {}", user.getEmail(), user.isOnboardingCompleted());

        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }

    private String generateToken(User user) {
        Instant now = Instant.now();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("fireview")
                .issuedAt(now)
                .expiresAt(now.plusMillis(jwtExpirationMs))
                .subject(user.getEmail())
                .claim("role", user.getRole().name())
                .claim("userId", user.getId())
                .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}

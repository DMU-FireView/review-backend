package com.example.fireview.domain.auth.oauth2;

import com.example.fireview.domain.user.entity.User;
import com.example.fireview.global.security.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;

    @Value("${oauth2.redirect-uri}")
    private String frontendRedirectUri;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        OAuth2UserPrincipal principal = (OAuth2UserPrincipal) authentication.getPrincipal();
        User user = principal.getUser();

        String token = jwtTokenProvider.generateToken(user);

        // JWT 토큰을 Query Param(?)으로 리다이렉트
        // 프론트엔드 OAuthCallbackPage가 queryParams로 파싱하므로 Fragment(#) 대신 Query Param 사용
        // 파라미터 이름: accessToken (프론트 oauth_callback_view_model.dart 규격)
        String redirectUrl = UriComponentsBuilder.fromUriString(frontendRedirectUri)
                .queryParam("accessToken", token)
                .queryParam("tokenType", "Bearer")
                .queryParam("email", user.getEmail())
                .queryParam("nickname", user.getNickname())
                .queryParam("onboarding", !user.isOnboardingCompleted())
                .build().toUriString();

        log.info("OAuth2 로그인 성공 - provider: {}, email: {}, nickname: {}, 온보딩 완료: {}",
                user.getProvider(), user.getEmail(), user.getNickname(), user.isOnboardingCompleted());

        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }
}

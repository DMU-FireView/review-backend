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
import java.nio.charset.StandardCharsets;

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
        String redirectUrl = buildRedirectUrl(user, token);

        log.info("OAuth2 로그인 성공 - provider: {}, email: {}, nickname: {}, 온보딩 완료: {}",
                user.getProvider(), user.getEmail(), user.getNickname(), user.isOnboardingCompleted());

        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }

    /**
     * 로그인 성공 후 프론트로 돌려보낼 주소.
     *
     * <p>JWT 를 Query Param 으로 싣는다. 프론트 OAuthCallbackPage 가 queryParams 로
     * 파싱하므로 Fragment(#) 가 아니라 Query Param 이어야 한다.
     * 파라미터 이름은 프론트 {@code oauth_callback_view_model.dart} 규격을 따른다.
     *
     * <p><b>{@code encode()} 를 빼면 안 된다.</b> 닉네임에 한글이 들어가면 Location 헤더에
     * 비ASCII 문자가 그대로 실리는데, HTTP 헤더는 ISO-8859-1 만 허용하므로 Tomcat 이
     * 헤더를 통째로 지운다. 그러면 Location 없는 302 가 나가 브라우저가 빈 화면에 멈추고,
     * 서버 로그만 보면 "로그인 성공" 으로 보여서 원인을 찾기 어렵다. 실제로 겪었다.
     * {@code toUriString()} 은 인코딩을 하지 않으므로 직접 불러야 한다.
     */
    String buildRedirectUrl(User user, String token) {
        return UriComponentsBuilder.fromUriString(frontendRedirectUri)
                .queryParam("accessToken", token)
                .queryParam("tokenType", "Bearer")
                .queryParam("email", user.getEmail())
                .queryParam("nickname", user.getNickname())
                .queryParam("onboarding", !user.isOnboardingCompleted())
                .encode(StandardCharsets.UTF_8)
                .build().toUriString();
    }

    /** 테스트에서 프론트 주소를 주입하기 위한 통로 */
    void setFrontendRedirectUri(String uri) {
        this.frontendRedirectUri = uri;
    }
}

package com.example.fireview.domain.auth.oauth2;

import com.example.fireview.domain.auth.cookie.RefreshTokenCookies;
import com.example.fireview.domain.auth.service.InMemoryRefreshTokenStore;
import com.example.fireview.domain.auth.service.RefreshTokenService;
import com.example.fireview.domain.user.entity.OAuthProvider;
import com.example.fireview.domain.user.entity.User;
import com.example.fireview.domain.user.repository.UserRepository;
import com.example.fireview.global.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.TestingAuthenticationToken;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** OAuth 로그인 성공 302 에 리프레시 쿠키가 실리고, 기존 쿼리 accessToken 은 그대로인지 본다 */
class OAuth2SuccessHandlerCookieTest {

    private static final Duration TTL = Duration.ofMinutes(30);

    @Test
    void 리다이렉트에_리프레시_쿠키를_심고_쿼리에는_싣지_않는다() throws Exception {
        User user = User.builder().id(11L).email("oauth@fireview.com").nickname("소셜")
                .provider(OAuthProvider.GOOGLE).build();
        JwtTokenProvider jwt = mock(JwtTokenProvider.class);
        when(jwt.generateToken(user)).thenReturn("access.jwt.token");
        UserRepository users = mock(UserRepository.class);
        when(users.findById(11L)).thenReturn(Optional.of(user));
        RefreshTokenService refreshTokens = new RefreshTokenService(
                new InMemoryRefreshTokenStore(), users, TTL, Duration.ofSeconds(15));
        RefreshTokenCookies cookies = new RefreshTokenCookies("review_rt", "/api/auth", true, "Lax", TTL);

        OAuth2SuccessHandler handler = new OAuth2SuccessHandler(jwt, refreshTokens, cookies);
        handler.setFrontendRedirectUri("https://re-view.kr/auth/callback");
        MockHttpServletResponse response = new MockHttpServletResponse();
        OAuth2UserPrincipal principal = new OAuth2UserPrincipal(user, Map.of());

        handler.onAuthenticationSuccess(new MockHttpServletRequest(), response,
                new TestingAuthenticationToken(principal, null));

        assertThat(response.getRedirectedUrl())
                .startsWith("https://re-view.kr/auth/callback?")
                .contains("accessToken=access.jwt.token")
                .doesNotContain("refresh");
        String setCookie = response.getHeader(HttpHeaders.SET_COOKIE);
        assertThat(setCookie)
                .startsWith("review_rt=")
                .contains("Path=/api/auth", "Max-Age=1800", "HttpOnly", "Secure", "SameSite=Lax")
                .doesNotContain("Domain=");

        // 심은 값으로 실제 refresh 가 된다
        String token = setCookie.substring("review_rt=".length(), setCookie.indexOf(';'));
        assertThat(refreshTokens.rotate(token).user()).isEqualTo(user);
    }
}

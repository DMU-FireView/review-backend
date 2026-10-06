package com.example.fireview.domain.auth.oauth2;

import com.example.fireview.domain.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 로그인 성공 후 Location 헤더에 실릴 주소를 검증한다.
 *
 * <p>실제로 터졌던 문제다. 닉네임이 한글이면 인코딩 없이 Location 헤더에 실리는데,
 * HTTP 헤더는 ISO-8859-1 만 허용하므로 Tomcat 이 헤더를 통째로 지운다.
 *
 * <pre>
 * The HTTP response header [Location] with value [...nickname=남정현...]
 * has been removed from the response because it is invalid
 * </pre>
 *
 * <p>Location 없는 302 가 나가 브라우저는 빈 화면에 멈춘다. 서버 로그에는
 * "OAuth2 로그인 성공" 이 찍혀 있어서 원인을 찾기 어려웠다.
 */
class OAuth2SuccessHandlerRedirectTest {

    private static final String FRONTEND = "https://re-view.kr/auth/callback";
    private static final String TOKEN = "eyJhbGciOiJIUzI1NiJ9.payload.signature";

    private OAuth2SuccessHandler handler;

    @BeforeEach
    void setUp() {
        handler = new OAuth2SuccessHandler(null);
        handler.setFrontendRedirectUri(FRONTEND);
    }

    private static User user(String nickname, String email) {
        return User.builder().id(4L).email(email).nickname(nickname).build();
    }

    @Test
    void 한글_닉네임이_퍼센트_인코딩된다() {
        String url = handler.buildRedirectUrl(user("남정현", "namjh3505@naver.com"), TOKEN);

        assertThat(url)
                .contains("nickname=%EB%82%A8%EC%A0%95%ED%98%84")
                .doesNotContain("남정현");
    }

    @Test
    void 주소_전체가_ASCII다() {
        // Location 헤더에 들어갈 수 있는 문자만 남아야 Tomcat 이 헤더를 지우지 않는다
        String url = handler.buildRedirectUrl(user("남정현 😀 홍길동", "테스트@naver.com"), TOKEN);

        assertThat(StandardCharsets.US_ASCII.newEncoder().canEncode(url))
                .as("Location 헤더에 비ASCII 문자가 들어가면 Tomcat 이 헤더를 제거한다: %s", url)
                .isTrue();
    }

    @Test
    void 영문_닉네임은_그대로_나간다() {
        String url = handler.buildRedirectUrl(user("tester", "a@b.com"), TOKEN);

        assertThat(url).contains("nickname=tester");
    }

    @Test
    void 프론트가_기대하는_파라미터가_모두_실린다() {
        String url = handler.buildRedirectUrl(user("남정현", "namjh3505@naver.com"), TOKEN);

        assertThat(url).startsWith(FRONTEND + "?");
        assertThat(url)
                .contains("accessToken=" + TOKEN)
                .contains("tokenType=Bearer")
                // @ 는 쿼리에서 합법 문자라 인코딩되지 않는다. 비ASCII 만 바뀌면 된다
                .contains("email=namjh3505@naver.com")
                .contains("onboarding=true");   // onboardingCompleted=false 의 반대값
    }

    @Test
    void 온보딩을_마친_사용자는_onboarding_false다() {
        User done = User.builder().id(5L).email("a@b.com").nickname("tester")
                .onboardingCompleted(true).build();

        assertThat(handler.buildRedirectUrl(done, TOKEN)).contains("onboarding=false");
    }
}

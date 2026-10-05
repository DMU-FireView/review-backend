package com.example.fireview.global.config;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 운영 OAuth2 콜백 주소가 "브라우저가 보고 있는 호스트"인지 고정한다.
 *
 * <p>실제로 터졌던 문제다. 콜백을 {@code {baseUrl}} 로 두면 프론트 프록시가 Host 를
 * API 호스트로 바꿔 넘기는 탓에 {@code https://api.re-view.kr/...} 로 풀린다.
 * 그런데 state 쿠키({@code oauth2_auth_request})는 Domain 속성 없이 내려가므로
 * 브라우저가 요청한 호스트({@code re-view.kr})에만 저장된다.
 * 콜백 호스트가 다르면 쿠키가 전송되지 않아 Spring 이 state 를 찾지 못하고
 * <b>소셜 로그인이 통째로 실패한다.</b> 운영에서 OAuth 가입자가 0명이었던 원인이다.
 *
 * <p>그래서 두 가지를 못 박는다.
 * <ul>
 *   <li>운영 콜백은 {@code {baseUrl}} 이 아니라 절대 주소여야 한다</li>
 *   <li>그 주소의 오리진은 사용자가 보는 프론트와 같아야 한다</li>
 * </ul>
 */
class OAuth2CallbackHostTest {

    private static final String PREFIX = "spring.security.oauth2.client.registration.";

    private static Properties prodProperties() throws Exception {
        Properties props = new Properties();
        try (InputStream in = OAuth2CallbackHostTest.class
                .getResourceAsStream("/application-prod.properties")) {
            assertThat(in).as("application-prod.properties 를 찾지 못했다").isNotNull();
            props.load(in);
        }
        return props;
    }

    private static String origin(String uri) {
        int slash = uri.indexOf('/', "https://".length());
        return slash < 0 ? uri : uri.substring(0, slash);
    }

    /** {@code ${VAR:기본값}} 에서 기본값만 꺼낸다 */
    private static String resolveDefault(String raw) {
        StringBuilder out = new StringBuilder();
        int i = 0;
        while (i < raw.length()) {
            int start = raw.indexOf("${", i);
            if (start < 0) {
                out.append(raw.substring(i));
                break;
            }
            out.append(raw, i, start);
            int end = raw.indexOf('}', start);
            String body = raw.substring(start + 2, end);
            int colon = body.indexOf(':');
            out.append(colon < 0 ? "" : body.substring(colon + 1));
            i = end + 1;
        }
        return out.toString();
    }

    @Test
    void 운영_콜백은_baseUrl_템플릿을_쓰지_않는다() throws Exception {
        Properties props = prodProperties();

        for (String provider : new String[]{"google", "naver"}) {
            String uri = props.getProperty(PREFIX + provider + ".redirect-uri");
            assertThat(uri)
                    .as("%s 콜백이 운영 설정에 없다", provider)
                    .isNotNull();
            assertThat(uri)
                    .as("%s 콜백이 {baseUrl} 이면 프록시 Host 를 따라가 API 호스트로 풀린다", provider)
                    .doesNotContain("{baseUrl}");
        }
    }

    @Test
    void 운영_콜백_오리진은_사용자가_보는_프론트와_같다() throws Exception {
        Properties props = prodProperties();

        // 로그인 성공 후 사용자를 돌려보내는 주소 = 브라우저가 보고 있는 호스트
        String frontend = resolveDefault(props.getProperty("app.mail.password-reset-base-url"));
        String frontendOrigin = origin(frontend);
        assertThat(frontendOrigin).startsWith("https://");

        for (String provider : new String[]{"google", "naver"}) {
            String callback = resolveDefault(props.getProperty(PREFIX + provider + ".redirect-uri"));

            assertThat(origin(callback))
                    .as("%s 콜백 호스트가 프론트와 다르면 state 쿠키가 전송되지 않는다", provider)
                    .isEqualTo(frontendOrigin);
            assertThat(callback).endsWith("/login/oauth2/code/" + provider);
        }
    }
}

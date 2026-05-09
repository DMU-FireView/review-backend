package com.example.fireview.global.config;

import org.apache.tomcat.util.http.Rfc6265CookieProcessor;
import org.apache.tomcat.util.http.SameSiteCookies;
import org.springframework.boot.web.embedded.tomcat.TomcatContextCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Tomcat 세션 쿠키 SameSite=None 설정
 *
 * 네이버/구글 OAuth2 콜백은 cross-site 리다이렉트이므로
 * 세션 쿠키(JSESSIONID)에 SameSite=None; Secure가 없으면
 * 브라우저가 콜백 시 쿠키를 차단 → authorization_request_not_found 발생.
 *
 * server.servlet.session.cookie.same-site 프로퍼티보다
 * Tomcat 레벨 설정이 더 확실하게 적용됨.
 */
@Configuration
public class TomcatConfig {

    @Bean
    public TomcatContextCustomizer sameSiteSessionCookieCustomizer() {
        return context -> {
            Rfc6265CookieProcessor processor = new Rfc6265CookieProcessor();
            processor.setSameSiteCookies(SameSiteCookies.NONE.getValue());
            context.setCookieProcessor(processor);
        };
    }
}

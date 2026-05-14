package com.example.fireview.global.config;

import org.apache.tomcat.util.http.Rfc6265CookieProcessor;
import org.apache.tomcat.util.http.SameSiteCookies;
import org.springframework.boot.web.embedded.tomcat.TomcatContextCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Tomcat 세션 쿠키 SameSite=None; Secure 설정
 *
 * 네이버/구글 OAuth2 콜백은 cross-site 리다이렉트이므로
 * JSESSIONID에 SameSite=None; Secure가 없으면 브라우저가 차단함.
 * SameSite=None은 반드시 Secure 속성과 함께 사용해야 유효함.
 */
@Configuration
public class TomcatConfig {

    @Bean
    public TomcatContextCustomizer sameSiteSessionCookieCustomizer() {
        return context -> {
            Rfc6265CookieProcessor processor = new Rfc6265CookieProcessor();
            // SameSite=None: cross-site 리다이렉트에서도 쿠키 전달 허용
            processor.setSameSiteCookies(SameSiteCookies.NONE.getValue());
            context.setCookieProcessor(processor);
        };
    }
}

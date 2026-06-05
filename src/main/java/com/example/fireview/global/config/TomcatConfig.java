package com.example.fireview.global.config;

import org.apache.tomcat.util.http.Rfc6265CookieProcessor;
import org.apache.tomcat.util.http.SameSiteCookies;
import org.springframework.boot.web.embedded.tomcat.TomcatContextCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Tomcat 레벨 JSESSIONID SameSite=None 설정
 *
 * [왜 이 방식이 반드시 필요한가]
 * Tomcat은 JSESSIONID를 HttpServletResponse.addCookie()를 거치지 않고
 * CoyoteResponse.addHeader("Set-Cookie", ...) 로 내부 직접 씁니다.
 * 서블릿 레벨 필터(SameSiteCookieFilter)는 이 경로를 가로챌 수 없습니다.
 *
 * Rfc6265CookieProcessor.setSameSiteCookies(NONE) 은 Tomcat의 내부
 * 쿠키 생성 단계에서 SameSite=None 을 추가하므로 JSESSIONID에도 적용됩니다.
 *
 * [실증 로그]
 * save   sessionId=F1D9F015... (state 저장 성공)
 * remove sessionId=(세션없음)  ← JSESSIONID가 브라우저에 SameSite=None 없이
 *                               발급되어 cross-site 콜백 시 전달 안 됨
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

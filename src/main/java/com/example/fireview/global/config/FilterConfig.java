package com.example.fireview.global.config;

import com.example.fireview.global.filter.SameSiteCookieFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * SameSiteCookieFilter를 Spring Security보다 먼저 실행되도록 명시 등록
 *
 * @Component + @Order 만으로는 Spring Security 필터 체인과의
 * 실행 순서가 보장되지 않는 경우가 있어 FilterRegistrationBean으로 명시함.
 * Spring Security 기본 order = -100 이므로 -200으로 설정.
 */
@Configuration
public class FilterConfig {

    @Bean
    public FilterRegistrationBean<SameSiteCookieFilter> sameSiteCookieFilterRegistration(
            SameSiteCookieFilter sameSiteCookieFilter) {
        FilterRegistrationBean<SameSiteCookieFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(sameSiteCookieFilter);
        registration.addUrlPatterns("/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        registration.setName("sameSiteCookieFilter");
        return registration;
    }
}

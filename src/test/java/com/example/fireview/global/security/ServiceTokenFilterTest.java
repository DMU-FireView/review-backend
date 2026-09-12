package com.example.fireview.global.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;

class ServiceTokenFilterTest {

    private static final String TOKEN = "secret-service-token";

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void 올바른_토큰이면_ROLE_SERVICE_인증을_세팅한다() throws Exception {
        ServiceTokenFilter filter = new ServiceTokenFilter(TOKEN);
        MockHttpServletRequest request = internalRequest();
        request.addHeader(ServiceTokenFilter.HEADER, TOKEN);
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getAuthorities())
                .extracting("authority")
                .containsExactly(ServiceTokenFilter.ROLE_SERVICE);
        assertThat(chain.getRequest()).as("체인이 계속 진행되어야 한다").isSameAs(request);
    }

    @Test
    void 토큰이_틀리면_인증을_세팅하지_않고_체인은_계속_진행한다() throws Exception {
        ServiceTokenFilter filter = new ServiceTokenFilter(TOKEN);
        MockHttpServletRequest request = internalRequest();
        request.addHeader(ServiceTokenFilter.HEADER, "wrong");
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        // 거부는 Spring Security 가 담당하므로 필터는 체인을 끊지 않는다
        assertThat(chain.getRequest()).isSameAs(request);
    }

    @Test
    void 헤더가_없으면_인증을_세팅하지_않는다() throws Exception {
        ServiceTokenFilter filter = new ServiceTokenFilter(TOKEN);
        MockHttpServletRequest request = internalRequest();

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void 서버에_토큰이_설정되지_않으면_어떤_값도_인증하지_않는다() throws Exception {
        ServiceTokenFilter filter = new ServiceTokenFilter("");
        MockHttpServletRequest request = internalRequest();
        request.addHeader(ServiceTokenFilter.HEADER, "");

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void internal_경로가_아니면_토큰이_맞아도_서비스_인증을_부여하지_않는다() throws Exception {
        ServiceTokenFilter filter = new ServiceTokenFilter(TOKEN);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/products/1");
        request.setRequestURI("/api/products/1");
        request.addHeader(ServiceTokenFilter.HEADER, TOKEN);

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    private static MockHttpServletRequest internalRequest() {
        MockHttpServletRequest request =
                new MockHttpServletRequest("POST", "/api/internal/webhooks/analysis-complete");
        request.setRequestURI("/api/internal/webhooks/analysis-complete");
        return request;
    }
}

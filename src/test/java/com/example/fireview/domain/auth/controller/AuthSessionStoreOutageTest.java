package com.example.fireview.domain.auth.controller;

import com.example.fireview.domain.auth.service.RefreshTokenStore;
import com.example.fireview.domain.user.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 리프레시 저장소(운영은 Redis)가 죽었을 때(#213 리뷰 P2-2).
 *
 * <p>처음 로그인하는 경로는 기존처럼 액세스 토큰만으로 성공하고, 이미 있는 세션을 다루는
 * refresh·logout 은 401 이 아니라 503 으로 실패해 쿠키를 지우지 않는다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthSessionStoreOutageTest {

    private static final String COOKIE = "review_rt";
    private static final String PASSWORD = "password123!";

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @MockitoBean RefreshTokenStore store;

    private String email;

    @BeforeEach
    void setUp() {
        RedisConnectionFailureException down = new RedisConnectionFailureException("redis down");
        when(store.save(anyString(), any(), any(), any(), anyBoolean())).thenThrow(down);
        when(store.consume(anyString(), any(), any())).thenThrow(down);
        email = "outage-" + UUID.randomUUID() + "@fireview.com";
    }

    @Test
    void 회원가입은_성공하고_쿠키만_빠진다() throws Exception {
        MvcResult result = signup()
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").doesNotExist())
                .andReturn();

        assertNoRefreshCookie(result);
        // 저장소 실패가 가입 트랜잭션을 되돌리지 않는다
        assertThat(userRepository.existsByEmail(email)).isTrue();
    }

    @Test
    void 로그인은_200_이고_쿠키도_본문_토큰도_없다() throws Exception {
        signup().andExpect(status().isCreated());

        MvcResult web = login(null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").doesNotExist())
                .andReturn();
        assertNoRefreshCookie(web);

        MvcResult app = login("app")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").doesNotExist())
                .andReturn();
        assertNoRefreshCookie(app);
    }

    @Test
    void refresh_는_503_이고_쿠키를_지우지_않는다() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/refresh").cookie(new Cookie(COOKIE, "some-token")))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("AUTH_SESSION_UNAVAILABLE"))
                .andReturn();

        assertNoRefreshCookie(result);
    }

    @Test
    void logout_은_성공으로_덮지_않고_503() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/logout").cookie(new Cookie(COOKIE, "some-token")))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.errorCode").value("AUTH_SESSION_UNAVAILABLE"))
                .andReturn();

        assertNoRefreshCookie(result);
    }

    private ResultActions signup() throws Exception {
        return mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"%s","password":"%s","nickname":"장애"}
                        """.formatted(email, PASSWORD)));
    }

    private ResultActions login(String platform) throws Exception {
        MockHttpServletRequestBuilder request = post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, PASSWORD));
        if (platform != null) {
            request.header("X-Client-Platform", platform);
        }
        return mockMvc.perform(request);
    }

    private static void assertNoRefreshCookie(MvcResult result) {
        assertThat(result.getResponse().getHeaders(HttpHeaders.SET_COOKIE))
                .noneMatch(h -> h.startsWith(COOKIE + "="));
    }
}

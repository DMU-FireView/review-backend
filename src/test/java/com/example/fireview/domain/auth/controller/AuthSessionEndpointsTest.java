package com.example.fireview.domain.auth.controller;

import com.example.fireview.domain.auth.service.PasswordResetTokenStore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 로그인 세션(리프레시 토큰) 엔드포인트를 실제 필터 체인·H2·인메모리 저장소로 검증한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthSessionEndpointsTest {

    private static final String COOKIE = "review_rt";
    private static final String PASSWORD = "password123!";

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired PasswordResetTokenStore passwordResetTokenStore;

    private String email;

    @BeforeEach
    void setUp() throws Exception {
        email = "rt-" + UUID.randomUUID() + "@fireview.com";
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s","nickname":"세션테스트"}
                                """.formatted(email, PASSWORD)))
                .andExpect(status().isCreated());
    }

    // ── 로그인·회원가입 ──────────────────────────────────────────────────────

    @Test
    void 로그인하면_기존_본문은_그대로이고_리프레시_쿠키가_심긴다() throws Exception {
        MvcResult result = login(null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.email").value(email))
                .andExpect(jsonPath("$.data.nickname").value("세션테스트"))
                .andExpect(jsonPath("$.data.role").value("USER"))
                .andExpect(jsonPath("$.data.onboardingCompleted").value(false))
                // 웹에는 절대 본문으로 주지 않는다
                .andExpect(jsonPath("$.data.refreshToken").doesNotExist())
                .andReturn();

        String setCookie = setCookie(result);
        assertThat(setCookie)
                .startsWith(COOKIE + "=")
                .contains("Path=/api/auth")
                .contains("Max-Age=1800")
                .contains("HttpOnly")
                .contains("Secure")
                .contains("SameSite=Lax")
                .doesNotContain("Domain=");
        assertThat(cookieValue(result)).hasSize(43);
    }

    @Test
    void 회원가입_응답에도_리프레시_쿠키가_심긴다() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"new-%s","password":"%s","nickname":"가입"}
                                """.formatted(email, PASSWORD)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").doesNotExist())
                .andReturn();

        assertThat(cookieValue(result)).hasSize(43);
    }

    @Test
    void 네이티브_앱이면_본문으로만_주고_쿠키는_심지_않는다() throws Exception {
        MvcResult result = login("app")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andReturn();

        assertThat(data(result).get("refreshToken").asText()).hasSize(43);
        assertNoRefreshCookie(result);
    }

    @Test
    void 브라우저_Origin_이_있으면_앱_헤더가_있어도_본문에_싣지_않는다() throws Exception {
        MvcResult result = mockMvc.perform(loginRequest()
                        .header("X-Client-Platform", "app")
                        .header(HttpHeaders.ORIGIN, "http://localhost:3000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").doesNotExist())
                .andReturn();

        assertThat(cookieValue(result)).hasSize(43);
    }

    @Test
    void Fetch_Metadata_가_있으면_앱_헤더가_있어도_본문에_싣지_않는다() throws Exception {
        MvcResult result = mockMvc.perform(loginRequest()
                        .header("X-Client-Platform", "app")
                        .header("Sec-Fetch-Site", "same-origin")
                        .header("Sec-Fetch-Mode", "cors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.refreshToken").doesNotExist())
                .andReturn();

        assertThat(cookieValue(result)).hasSize(43);
    }

    @Test
    void 회원가입도_브라우저_요청이면_앱_헤더가_있어도_본문에_싣지_않는다() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/signup")
                        .header("X-Client-Platform", "app")
                        .header("Sec-Fetch-Site", "same-origin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"web-%s","password":"%s","nickname":"웹가입"}
                                """.formatted(email, PASSWORD)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.refreshToken").doesNotExist())
                .andReturn();

        assertThat(cookieValue(result)).hasSize(43);
    }

    // ── refresh ────────────────────────────────────────────────────────────

    @Test
    void refresh_하면_새_액세스_토큰과_회전된_쿠키를_준다() throws Exception {
        String old = cookieValue(login(null).andReturn());

        MvcResult result = mockMvc.perform(post("/api/auth/refresh").cookie(new Cookie(COOKIE, old)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.email").value(email))
                .andExpect(jsonPath("$.data.refreshToken").doesNotExist())
                .andReturn();

        String rotated = cookieValue(result);
        assertThat(rotated).hasSize(43).isNotEqualTo(old);
        assertThat(setCookie(result)).contains("Max-Age=1800").contains("HttpOnly");

        // 새 액세스 토큰으로 인증이 필요한 API 를 부를 수 있다
        String accessToken = data(result).get("accessToken").asText();
        mockMvc.perform(get("/api/users/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value(email));

        // 회전된 값으로 다시 refresh 할 수 있다
        mockMvc.perform(post("/api/auth/refresh").cookie(new Cookie(COOKIE, rotated)))
                .andExpect(status().isOk());
    }

    @Test
    void 앱은_본문_refreshToken_으로_refresh_하고_본문으로만_새_값을_받는다() throws Exception {
        String token = data(login("app").andReturn()).get("refreshToken").asText();

        MvcResult result = mockMvc.perform(post("/api/auth/refresh")
                        .header("X-Client-Platform", "app")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"%s\"}".formatted(token)))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(data(result).get("refreshToken").asText()).hasSize(43).isNotEqualTo(token);
        assertNoRefreshCookie(result);
    }

    @Test
    void 쿠키로_refresh_하면_앱_헤더를_붙여도_본문에_토큰을_싣지_않는다() throws Exception {
        // 웹 XSS 가 fetch('/api/auth/refresh', {headers:{'X-Client-Platform':'app'}}) 로
        // HttpOnly 쿠키 값을 읽어 가던 경로(#213 리뷰 P2-1)
        String old = cookieValue(login(null).andReturn());

        MvcResult result = mockMvc.perform(post("/api/auth/refresh")
                        .header("X-Client-Platform", "app")
                        .cookie(new Cookie(COOKIE, old)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").doesNotExist())
                .andReturn();

        assertThat(cookieValue(result)).hasSize(43).isNotEqualTo(old);
    }

    @Test
    void 쿠키가_없으면_401_과_쿠키_삭제() throws Exception {
        expectRefreshRejected(mockMvc.perform(post("/api/auth/refresh")));
    }

    @Test
    void 모르는_쿠키면_401_과_쿠키_삭제() throws Exception {
        expectRefreshRejected(mockMvc.perform(post("/api/auth/refresh").cookie(new Cookie(COOKIE, "forged"))));
    }

    // ── logout ─────────────────────────────────────────────────────────────

    @Test
    void 로그아웃하면_쿠키가_지워지고_그_토큰으로_refresh_할_수_없다() throws Exception {
        String token = cookieValue(login(null).andReturn());

        MvcResult result = mockMvc.perform(post("/api/auth/logout").cookie(new Cookie(COOKIE, token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();
        assertThat(setCookie(result)).startsWith(COOKIE + "=;").contains("Max-Age=0").contains("Path=/api/auth");

        expectRefreshRejected(mockMvc.perform(post("/api/auth/refresh").cookie(new Cookie(COOKIE, token))));
    }

    @Test
    void 로그아웃은_토큰이_없거나_이미_무효여도_200() throws Exception {
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/auth/logout").cookie(new Cookie(COOKIE, "already-gone")))
                .andExpect(status().isOk());
    }

    // ── 전체 폐기 ──────────────────────────────────────────────────────────

    @Test
    void 비밀번호를_재설정하면_모든_리프레시_토큰이_폐기된다() throws Exception {
        String laptop = cookieValue(login(null).andReturn());
        String phone = data(login("app").andReturn()).get("refreshToken").asText();

        String resetToken = passwordResetTokenStore.issue(email);
        mockMvc.perform(post("/api/auth/password/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"%s\",\"newPassword\":\"newPassword123!\"}".formatted(resetToken)))
                .andExpect(status().isOk());

        expectRefreshRejected(mockMvc.perform(post("/api/auth/refresh").cookie(new Cookie(COOKIE, laptop))));
        expectRefreshRejected(mockMvc.perform(post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"%s\"}".formatted(phone))));
    }

    @Test
    void 회원_탈퇴하면_리프레시_토큰이_폐기된다() throws Exception {
        MvcResult login = login(null).andReturn();
        String token = cookieValue(login);
        String accessToken = data(login).get("accessToken").asText();

        mockMvc.perform(delete("/api/users/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isNoContent());

        expectRefreshRejected(mockMvc.perform(post("/api/auth/refresh").cookie(new Cookie(COOKIE, token))));
    }

    // ── helpers ────────────────────────────────────────────────────────────

    private ResultActions login(String platform) throws Exception {
        MockHttpServletRequestBuilder request = loginRequest();
        if (platform != null) {
            request.header("X-Client-Platform", platform);
        }
        return mockMvc.perform(request);
    }

    private MockHttpServletRequestBuilder loginRequest() {
        return post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, PASSWORD));
    }

    private static void assertNoRefreshCookie(MvcResult result) {
        assertThat(result.getResponse().getHeaders(HttpHeaders.SET_COOKIE))
                .noneMatch(h -> h.startsWith(COOKIE + "="));
    }

    private void expectRefreshRejected(ResultActions result) throws Exception {
        MvcResult mvcResult = result
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("REFRESH_TOKEN_INVALID"))
                .andReturn();
        assertThat(setCookie(mvcResult)).startsWith(COOKIE + "=;").contains("Max-Age=0");
    }

    private JsonNode data(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("data");
    }

    private static String setCookie(MvcResult result) {
        List<String> headers = result.getResponse().getHeaders(HttpHeaders.SET_COOKIE);
        List<String> ours = headers.stream().filter(h -> h.startsWith(COOKIE + "=")).toList();
        assertThat(ours).as("리프레시 쿠키 Set-Cookie 는 정확히 하나여야 한다: %s", headers).hasSize(1);
        return ours.get(0);
    }

    private static String cookieValue(MvcResult result) {
        String header = setCookie(result);
        return header.substring(COOKIE.length() + 1, header.indexOf(';'));
    }
}

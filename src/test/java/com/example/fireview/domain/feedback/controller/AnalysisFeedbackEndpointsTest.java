package com.example.fireview.domain.feedback.controller;

import com.example.fireview.domain.feedback.dto.response.AnalysisFeedbackResponse;
import com.example.fireview.domain.feedback.entity.AnalysisFeedbackStatus;
import com.example.fireview.domain.feedback.entity.AnalysisFeedbackType;
import com.example.fireview.domain.feedback.service.AnalysisFeedbackService;
import com.example.fireview.domain.user.entity.OAuthProvider;
import com.example.fireview.domain.user.entity.Role;
import com.example.fireview.domain.user.entity.User;
import com.example.fireview.global.exception.CustomException;
import com.example.fireview.global.exception.ErrorCode;
import com.example.fireview.global.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 외부 리뷰 분석 피드백 경로를 실제 필터 체인으로 검증한다.
 * 서비스는 목이라 Data 서버 없이 돈다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AnalysisFeedbackEndpointsTest {

    private static final String EMAIL = "auth-test@fireview.com";
    private static final String URL = "/api/analysis-feedbacks/external/kurly/1000146248/reviews/r-1";
    private static final String BODY = """
            { "feedbackType": "SCORE_MISMATCH", "relatedSignals": ["repetition"], "detail": "점수가 낮아요" }
            """;

    @Autowired MockMvc mockMvc;
    @Autowired JwtTokenProvider jwtTokenProvider;

    @MockitoBean AnalysisFeedbackService analysisFeedbackService;

    private String bearerToken;

    @BeforeEach
    void setUp() {
        bearerToken = "Bearer " + jwtTokenProvider.generateToken(User.builder()
                .id(1L).email(EMAIL).nickname("인증테스트").role(Role.USER).provider(OAuthProvider.LOCAL)
                .createdAt(LocalDateTime.now()).build());
    }

    @Test
    void 토큰_없이_호출하면_401() throws Exception {
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));

        verifyNoInteractions(analysisFeedbackService);
    }

    @Test
    void 인증되면_경로의_식별자를_문자열_그대로_넘기고_외부_식별자를_돌려준다() throws Exception {
        when(analysisFeedbackService.submitExternal(anyString(), anyString(), anyString(), anyString(), any()))
                .thenReturn(new AnalysisFeedbackResponse(7L, null, null, "토리든 마스크팩",
                        AnalysisFeedbackType.SCORE_MISMATCH, AnalysisFeedbackType.SCORE_MISMATCH.getDescription(),
                        null, List.of("repetition"), "점수가 낮아요", null, null,
                        AnalysisFeedbackStatus.SUBMITTED, AnalysisFeedbackStatus.SUBMITTED.getDescription(),
                        LocalDateTime.now(), LocalDateTime.now(), "r-1", "kurly-1000146248"));

        mockMvc.perform(post(URL).header(HttpHeaders.AUTHORIZATION, bearerToken)
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.feedbackId").value(7))
                .andExpect(jsonPath("$.data.reviewId").doesNotExist())
                .andExpect(jsonPath("$.data.externalReviewId").value("r-1"))
                .andExpect(jsonPath("$.data.productExternalId").value("kurly-1000146248"));

        verify(analysisFeedbackService).submitExternal(
                eq("kurly"), eq("1000146248"), eq("r-1"), eq(EMAIL), any());
    }

    @Test
    void 피드백_유형이_없으면_400() throws Exception {
        mockMvc.perform(post(URL).header(HttpHeaders.AUTHORIZATION, bearerToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{ \"detail\": \"유형 없음\" }"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(analysisFeedbackService);
    }

    @Test
    void 수집_전_상품이면_409() throws Exception {
        when(analysisFeedbackService.submitExternal(anyString(), anyString(), anyString(), anyString(), any()))
                .thenThrow(new CustomException(ErrorCode.PRODUCT_NOT_COLLECTED));

        mockMvc.perform(post(URL).header(HttpHeaders.AUTHORIZATION, bearerToken)
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("PRODUCT_NOT_COLLECTED"));
    }
}

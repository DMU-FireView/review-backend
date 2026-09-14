package com.example.fireview.domain.webhook.controller;

import com.example.fireview.domain.webhook.service.WebhookService;
import com.example.fireview.global.security.ServiceTokenFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SecurityConfig + ServiceTokenFilter + 컨트롤러 배선을 한 번에 검증한다.
 * 테스트 프로필의 app.service-token=test-service-token 을 사용한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WebhookControllerTest {

    private static final String URL = "/api/internal/webhooks/analysis-complete";
    private static final String VALID_BODY = """
            {
              "jobId": "job-100",
              "productId": "p-1",
              "productName": "테스트 상품",
              "requesterEmail": "user@test.com",
              "status": "COMPLETED"
            }
            """;

    @Autowired MockMvc mockMvc;
    @MockitoBean WebhookService webhookService;

    @Test
    void 서비스_토큰이_없으면_401() throws Exception {
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));

        verify(webhookService, never()).handleAnalysisComplete(any());
    }

    @Test
    void 서비스_토큰이_틀리면_401() throws Exception {
        mockMvc.perform(post(URL)
                        .header(ServiceTokenFilter.HEADER, "wrong-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isUnauthorized());

        verify(webhookService, never()).handleAnalysisComplete(any());
    }

    @Test
    void 올바른_토큰이면_200_이고_서비스가_호출된다() throws Exception {
        mockMvc.perform(post(URL)
                        .header(ServiceTokenFilter.HEADER, "test-service-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(webhookService).handleAnalysisComplete(any());
    }

    @Test
    void 필수값이_빠지면_400() throws Exception {
        String missingJobId = """
                { "productId": "p-1", "requesterEmail": "user@test.com", "status": "COMPLETED" }
                """;

        mockMvc.perform(post(URL)
                        .header(ServiceTokenFilter.HEADER, "test-service-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(missingJobId))
                .andExpect(status().isBadRequest());

        verify(webhookService, never()).handleAnalysisComplete(any());
    }
}

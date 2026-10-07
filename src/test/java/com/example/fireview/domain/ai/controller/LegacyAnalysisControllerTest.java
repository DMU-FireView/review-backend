package com.example.fireview.domain.ai.controller;

import com.example.fireview.domain.ai.client.AiServerClient;
import com.example.fireview.domain.ai.dto.response.AiProductListResponse;
import com.example.fireview.domain.ai.dto.response.AiProductSummary;
import com.example.fireview.domain.ai.service.AiAnalysisService;
import com.example.fireview.domain.notification.service.NotificationService;
import com.example.fireview.domain.product.entity.Product;
import com.example.fireview.domain.product.repository.ProductRepository;
import com.example.fireview.domain.review.repository.ReviewRepository;
import com.example.fireview.domain.user.entity.User;
import com.example.fireview.domain.user.service.UserService;
import com.example.fireview.global.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class LegacyAnalysisControllerTest {
    private final AiServerClient client = mock(AiServerClient.class);
    private final ProductRepository products = mock(ProductRepository.class);
    private final ReviewRepository reviews = mock(ReviewRepository.class);
    private final NotificationService notifications = mock(NotificationService.class);
    private final UserService users = mock(UserService.class);
    private final RestTemplate rest = mock(RestTemplate.class);
    private final com.example.fireview.domain.ai.support.TestTransactionManager transactions =
            new com.example.fireview.domain.ai.support.TestTransactionManager();
    private AiAnalysisService service;
    private MockMvc mvc;

    @BeforeEach
    void setup() {
        service = new AiAnalysisService(client, products, reviews, notifications, users, Runnable::run, transactions);
        AiHealthController health = new AiHealthController(rest);
        ReflectionTestUtils.setField(health, "aiServerBaseUrl", "http://private-host:8000");
        mvc = MockMvcBuilders.standaloneSetup(new AiAnalysisController(service), health)
                .setCustomArgumentResolvers(new org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void allFailuresReturn503WithoutDatabaseOrNotificationEffects() throws Exception {
        when(client.analyzeProductList(any())).thenThrow(new IllegalStateException("unavailable"));
        when(client.analyzeProductDetail(any())).thenThrow(new IllegalStateException("unavailable"));
        when(client.analyzeRtiTrend(any())).thenThrow(new IllegalStateException("unavailable"));
        when(client.analyzeProductRiskReport(any())).thenThrow(new IllegalStateException("unavailable"));
        mvc.perform(post("/api/analysis/product").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":\"p-1\"}"))
                .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("AI_ANALYSIS_UNAVAILABLE"));
        assertThat(org.assertj.core.api.Assertions.catchThrowable(() -> service.analyzeProduct("p-1", null, "user@test.com")))
                .isInstanceOf(com.example.fireview.global.exception.CustomException.class);
        verifyNoInteractions(products, reviews, notifications, users);
        assertThat(transactions.commits()).isZero();
    }

    @Test
    void partialSuccessReturns200AndNotifiesLoggedInRequesterOnce() throws Exception {
        when(client.analyzeProductList(any())).thenAnswer(invocation -> {
            assertThat(org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            return new AiProductListResponse(List.of(new AiProductSummary("p-1", 82.0, "safe", 10, 8, 1, 1)));
        });
        when(client.analyzeProductDetail(any())).thenThrow(new IllegalStateException("unavailable"));
        Product product = Product.builder().name("상품").build();
        when(products.findByNaverProductId("p-1")).thenReturn(Optional.of(product));
        when(products.save(product)).thenAnswer(invocation -> {
            assertThat(org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
            return product;
        });
        when(users.findByEmail("user@test.com")).thenReturn(mock(User.class));
        Jwt jwt = Jwt.withTokenValue("test").header("alg", "none").subject("user@test.com").build();
        ResponseEntity<?> response = new AiAnalysisController(service).analyzeProduct(
                new com.example.fireview.domain.ai.dto.request.ProductAnalyzeRequest("p-1", null), jwt);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(product.getAvgRti()).isEqualTo(82.0);
        assertThat(transactions.commits()).isEqualTo(1);
        verify(products).save(product);
        verify(notifications).createNotification(any(), any(), any(), any(), any());
        mvc.perform(post("/api/analysis/product").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":\"p-1\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.averageRti").value(82.0));
    }

    @Test
    void unreachableHealthReturnsSanitized503() throws Exception {
        when(rest.getForEntity("http://private-host:8000", String.class))
                .thenThrow(new ResourceAccessException("secret socket exception http://private-host:8000"));
        String body = mvc.perform(get("/api/analysis/health"))
                .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("AI_ANALYSIS_UNAVAILABLE"))
                .andReturn().getResponse().getContentAsString();
        assertThat(body).doesNotContain("private-host", "secret", "socket", "aiServerUrl");
    }

    @ParameterizedTest
    @ValueSource(ints = {400, 404, 500, 503})
    void upstreamHttpExceptionsReturnSanitized503(int statusCode) throws Exception {
        org.springframework.web.client.RestClientResponseException failure = statusCode < 500
                ? org.springframework.web.client.HttpClientErrorException.create(HttpStatus.valueOf(statusCode),
                    "secret upstream exception", null, "private-host".getBytes(), null)
                : org.springframework.web.client.HttpServerErrorException.create(HttpStatus.valueOf(statusCode),
                    "secret upstream exception", null, "private-host".getBytes(), null);
        when(rest.getForEntity("http://private-host:8000", String.class)).thenThrow(failure);
        String body = mvc.perform(get("/api/analysis/health"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.errorCode").value("AI_ANALYSIS_UNAVAILABLE"))
                .andReturn().getResponse().getContentAsString();
        assertThat(body).doesNotContain("private-host", "secret", "aiServerUrl");
    }

    @ParameterizedTest
    @ValueSource(ints = {200, 204, 302, 400, 404, 500, 503})
    void healthAcceptsOnly2xx(int statusCode) throws Exception {
        when(rest.getForEntity("http://private-host:8000", String.class))
                .thenReturn(ResponseEntity.status(statusCode).body("upstream"));
        if (statusCode >= 200 && statusCode < 300) {
            mvc.perform(get("/api/analysis/health")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("ok"))
                    .andExpect(jsonPath("$.data.aiServerUrl").doesNotExist());
        } else {
            mvc.perform(get("/api/analysis/health")).andExpect(status().isServiceUnavailable())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }
}

package com.example.fireview.domain.ai.controller;

import com.example.fireview.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * AI 서버 연결 상태 확인 컨트롤러
 *
 * ngrok 연동 테스트 시 AI 서버와의 통신 여부를 빠르게 확인할 수 있습니다.
 *
 * [테스트 방법]
 * 1. application.properties에서 ai.server.base-url을 ngrok URL로 변경
 *    예) ai.server.base-url=https://xxxx-xx-xx.ngrok.io
 * 2. GET /api/analysis/health 호출 → AI 서버 응답 확인
 */
@Slf4j
@RestController
@RequestMapping("/api/analysis")
@RequiredArgsConstructor
public class AiHealthController {

    private final RestTemplate restTemplate;

    @Value("${ai.server.base-url}")
    private String aiServerBaseUrl;

    /**
     * AI 서버 연결 상태 확인
     *
     * GET /api/analysis/health
     *
     * @return AI 서버 연결 정보 및 상태
     */
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkAiServerHealth() {
        log.info("[AI Health] AI 서버 연결 확인: baseUrl={}", aiServerBaseUrl);

        String status = "unknown";
        String message = "";
        long responseTimeMs = -1;

        long startTime = System.currentTimeMillis();
        try {
            String healthUrl = aiServerBaseUrl + "/health";
            restTemplate.getForObject(healthUrl, String.class);
            responseTimeMs = System.currentTimeMillis() - startTime;
            status = "ok";
            message = "AI 서버 연결 성공";
            log.info("[AI Health] 연결 성공: {}ms", responseTimeMs);
        } catch (Exception e) {
            responseTimeMs = System.currentTimeMillis() - startTime;
            status = "error";
            message = "AI 서버 연결 실패: " + e.getMessage();
            log.warn("[AI Health] 연결 실패: {}", e.getMessage());
        }

        Map<String, Object> result = Map.of(
                "status", status,
                "message", message,
                "aiServerUrl", aiServerBaseUrl,
                "responseTimeMs", responseTimeMs,
                "checkedAt", LocalDateTime.now().toString()
        );

        return ResponseEntity.ok(ApiResponse.success(result));
    }
}

package com.example.fireview.domain.ai.controller;

import com.example.fireview.global.exception.CustomException;
import com.example.fireview.global.exception.ErrorCode;
import com.example.fireview.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 레거시 서버 연결 상태 확인 컨트롤러.
 * 새 구조에서는 Data 서버가 ai.re-view.kr 를 호출. 이 경로는 프론트 옛 상세 화면 호환용.
 */
@Slf4j
@Tag(name = "AI 분석", description = "레거시: 새 구조에서는 Data 서버가 ai.re-view.kr 를 호출. 이 경로는 프론트 옛 상세 화면 호환용.")
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
    @Operation(description = "레거시 서버의 2xx 응답일 때만 status ok를 반환한다. 미도달 또는 비정상 응답은 내부 주소와 예외 원문 없이 공통 503 오류로 반환한다.")
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkAiServerHealth() {
        log.info("[AI Health] AI 서버 연결 확인: baseUrl={}", aiServerBaseUrl);

        long startTime = System.currentTimeMillis();
        ResponseEntity<String> upstream;
        try {
            upstream = restTemplate.getForEntity(aiServerBaseUrl, String.class);
        } catch (Exception e) {
            log.warn("[AI Health] 연결 실패: {}", e.getMessage());
            throw new CustomException(ErrorCode.AI_ANALYSIS_UNAVAILABLE);
        }
        if (!upstream.getStatusCode().is2xxSuccessful()) {
            throw new CustomException(ErrorCode.AI_ANALYSIS_UNAVAILABLE);
        }

        Map<String, Object> result = Map.of(
                "status", "ok",
                "message", "AI 서버 연결 성공",
                "responseTimeMs", System.currentTimeMillis() - startTime,
                "checkedAt", LocalDateTime.now().toString()
        );

        return ResponseEntity.ok(ApiResponse.success(result));
    }
}

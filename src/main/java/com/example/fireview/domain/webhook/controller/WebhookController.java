package com.example.fireview.domain.webhook.controller;

import com.example.fireview.domain.webhook.dto.request.AnalysisCompleteWebhookRequest;
import com.example.fireview.domain.webhook.service.WebhookService;
import com.example.fireview.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 서버 간 웹훅 수신 엔드포인트.
 * /api/internal/** 은 ServiceTokenFilter 가 검증한 X-Service-Token 이 있어야 접근된다.
 */
@RestController
@RequestMapping("/api/internal/webhooks")
@RequiredArgsConstructor
public class WebhookController {

    private final WebhookService webhookService;

    /**
     * 분석 job 완료/실패 웹훅 (Data 서버 → Spring)
     * POST /api/internal/webhooks/analysis-complete
     *
     * 중복 jobId 도 200 을 반환한다. Data 서버는 2xx 를 받으면 재전송을 멈추면 된다.
     */
    @PostMapping("/analysis-complete")
    public ApiResponse<Void> analysisComplete(@Valid @RequestBody AnalysisCompleteWebhookRequest request) {
        webhookService.handleAnalysisComplete(request);
        return ApiResponse.ok("웹훅이 처리되었습니다.");
    }
}

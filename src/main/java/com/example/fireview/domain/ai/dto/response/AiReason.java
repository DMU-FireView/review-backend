package com.example.fireview.domain.ai.dto.response;

/**
 * AI 판단 사유 코드 및 메시지
 */
public record AiReason(
        String code,    // 예: EXCESSIVE_EXCLAMATION
        String message  // 예: 과도한 느낌표 사용
) {}

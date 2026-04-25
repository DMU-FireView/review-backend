package com.example.fireview.domain.ai.dto.response;

/**
 * AI 분석 세부 점수 (3개 분야)
 */
public record AiSignals(
        Integer text,       // 텍스트 진정성 점수 (0~100)
        Integer behavior,   // 작성자 행동 패턴 점수 (0~100)
        Integer network     // 네트워크 군집 조작 점수 (0~100)
) {}

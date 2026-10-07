package com.example.fireview.domain.ai.dto.response;

/**
 * 레거시: 새 구조에서는 Data 서버가 ai.re-view.kr 를 호출. 이 경로는 프론트 옛 상세 화면 호환용.
 * AI 판단 사유 코드 및 메시지
 */
public record AiReason(
        String code,    // 예: EXCESSIVE_EXCLAMATION
        String message  // 예: 과도한 느낌표 사용
) {}

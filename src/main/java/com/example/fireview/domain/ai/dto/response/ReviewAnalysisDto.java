package com.example.fireview.domain.ai.dto.response;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 프론트엔드 전달용 개별 리뷰 분석 결과 DTO
 *
 * AI 서버 응답(AiAnalysisResult)에서 프론트에 필요한 필드만 선별합니다.
 * - input_features (AI 내부 디버그 필드) 제거
 * - null 기본값 보장
 * - 사유(reasons)는 메시지 문자열 목록으로 정제
 */
public record ReviewAnalysisDto(

        String reviewId,
        Integer rti,         // RTI 점수 (0~100)
        String level,        // safe | warn | danger
        Integer textScore,   // 텍스트 진정성 점수
        Integer behaviorScore, // 작성자 행동 패턴 점수
        Integer networkScore,  // 네트워크 군집 점수
        List<String> reasons   // 판정 사유 메시지 목록

) {
    /** AI 서버 응답으로부터 프론트용 DTO 생성 (null-safe) */
    public static ReviewAnalysisDto from(AiAnalysisResult result) {
        List<String> reasonMessages = (result.reasons() != null)
                ? result.reasons().stream()
                        .map(AiReason::message)
                        .filter(msg -> msg != null && !msg.isBlank())
                        .collect(Collectors.toList())
                : List.of();

        return new ReviewAnalysisDto(
                result.reviewId() != null ? result.reviewId() : "",
                result.rti() != null ? result.rti() : 50,
                result.level() != null ? result.level() : "warn",
                result.signals() != null ? result.signals().text() : null,
                result.signals() != null ? result.signals().behavior() : null,
                result.signals() != null ? result.signals().network() : null,
                reasonMessages
        );
    }
}

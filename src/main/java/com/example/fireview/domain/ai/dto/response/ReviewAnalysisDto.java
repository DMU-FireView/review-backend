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
        String content,        // 리뷰 본문
        String author,         // 작성자 마스킹 ID
        String date,           // 작성일 (YYYY.MM.DD)
        Integer rti,           // RTI 점수 (0~100)
        String level,          // safe | warn | danger
        Integer textScore,     // 텍스트 진정성 점수
        Integer behaviorScore, // 작성자 행동 패턴 점수
        Integer networkScore,  // 네트워크 군집 점수
        List<String> reasons   // 판정 사유 메시지 목록

) {
    /** risk-report sample_reviews 기반으로 content/author/date 포함 DTO 생성 */
    public static ReviewAnalysisDto from(SampleReview sample, AiAnalysisResult result) {
        List<String> reasonMessages = (sample.reasons() != null)
                ? sample.reasons().stream()
                        .map(AiReason::message)
                        .filter(msg -> msg != null && !msg.isBlank())
                        .collect(Collectors.toList())
                : List.of();

        int rti = (result != null && result.rti() != null) ? result.rti()
                : switch (sample.level() != null ? sample.level() : "warn") {
                    case "safe" -> 85; case "danger" -> 30; default -> 55;
                };

        return new ReviewAnalysisDto(
                sample.reviewId(),
                sample.content(),
                sample.author(),
                sample.date(),
                rti,
                sample.level() != null ? sample.level() : "warn",
                result != null && result.signals() != null ? result.signals().text() : null,
                result != null && result.signals() != null ? result.signals().behavior() : null,
                result != null && result.signals() != null ? result.signals().network() : null,
                reasonMessages
        );
    }

    /** product-detail 결과만 있을 때 (content 없음) */
    public static ReviewAnalysisDto from(AiAnalysisResult result) {
        List<String> reasonMessages = (result.reasons() != null)
                ? result.reasons().stream()
                        .map(AiReason::message)
                        .filter(msg -> msg != null && !msg.isBlank())
                        .collect(Collectors.toList())
                : List.of();

        return new ReviewAnalysisDto(
                result.reviewId() != null ? result.reviewId() : "",
                null, null, null,
                result.rti() != null ? result.rti() : 50,
                result.level() != null ? result.level() : "warn",
                result.signals() != null ? result.signals().text() : null,
                result.signals() != null ? result.signals().behavior() : null,
                result.signals() != null ? result.signals().network() : null,
                reasonMessages
        );
    }
}

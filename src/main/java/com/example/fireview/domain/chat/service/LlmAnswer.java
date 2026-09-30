package com.example.fireview.domain.chat.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

/**
 * LLM 이 돌려준 구조화 응답.
 *
 * 시스템 프롬프트가 {"onTopic": bool, "answer": "..."} 형식을 요구한다.
 * 형식이 깨져도 서비스가 죽으면 안 되므로, 파싱 실패 시 원문을 답변으로 쓰되
 * 주제 판정은 통과시키지 않는다(보수적으로 처리).
 */
@Slf4j
public record LlmAnswer(boolean onTopic, String answer) {

    /** 모델이 형식을 어겼을 때 사용자에게 보여줄 문구 */
    private static final String MALFORMED_FALLBACK =
            "답변을 정리하지 못했습니다. 상품이나 리뷰에 대해 다시 질문해주세요.";

    public static LlmAnswer parse(String raw, ObjectMapper objectMapper) {
        if (raw == null || raw.isBlank()) {
            return new LlmAnswer(false, MALFORMED_FALLBACK);
        }
        String json = extractJsonObject(raw);
        if (json == null) {
            log.warn("[Chat] 응답에서 JSON 을 찾지 못했다. 원문 길이={}", raw.length());
            return new LlmAnswer(false, MALFORMED_FALLBACK);
        }
        try {
            JsonNode node = objectMapper.readTree(json);
            JsonNode answerNode = node.get("answer");
            if (answerNode == null || answerNode.asText().isBlank()) {
                return new LlmAnswer(false, MALFORMED_FALLBACK);
            }
            boolean onTopic = node.path("onTopic").asBoolean(false);
            return new LlmAnswer(onTopic, answerNode.asText().trim());
        } catch (Exception e) {
            log.warn("[Chat] 구조화 응답 파싱 실패: {}", e.getMessage());
            return new LlmAnswer(false, MALFORMED_FALLBACK);
        }
    }

    /**
     * 응답에서 첫 번째 완결된 JSON 객체를 뽑아낸다.
     * 모델이 코드펜스(```json)나 설명을 덧붙이는 경우를 흡수한다.
     */
    private static String extractJsonObject(String raw) {
        int start = raw.indexOf('{');
        if (start < 0) return null;

        int depth = 0;
        boolean inString = false;
        boolean escaped = false;
        for (int i = start; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (escaped) {
                escaped = false;
                continue;
            }
            if (c == '\\') {
                escaped = true;
            } else if (c == '"') {
                inString = !inString;
            } else if (!inString) {
                if (c == '{') depth++;
                else if (c == '}' && --depth == 0) return raw.substring(start, i + 1);
            }
        }
        return null;
    }
}

package com.example.fireview.domain.chat.client;

import java.util.List;

/**
 * LLM 호출 추상화.
 *
 * 제공사(Anthropic / 게이트웨이)를 서비스 계층에서 분리해,
 * 모델이나 게이트웨이가 바뀌어도 ChatService 는 그대로 두기 위한 경계다.
 */
public interface LlmClient {

    /**
     * 단발 응답을 생성한다.
     *
     * @param systemPrompt 시스템 프롬프트
     * @param history      이전 대화 (오래된 것부터)
     * @param userMessage  이번 사용자 메시지 (분석 컨텍스트가 앞에 붙어 있을 수 있음)
     * @return 응답 본문과 토큰 사용량
     */
    LlmResponse complete(String systemPrompt, List<Turn> history, String userMessage);

    /** 실제 호출 없이 프롬프트의 입력 토큰 수만 센다. 쿼터 견적용 */
    int countTokens(String systemPrompt, List<Turn> history, String userMessage);

    /** 대화 한 턴 */
    record Turn(boolean fromUser, String content) {}

    /**
     * @param text         모델 응답 원문
     * @param inputTokens  입력 토큰
     * @param outputTokens 출력 토큰
     */
    record LlmResponse(String text, int inputTokens, int outputTokens) {}
}

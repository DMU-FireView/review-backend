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
    default LlmResponse complete(String systemPrompt, List<Turn> history, String userMessage) {
        return complete(systemPrompt, history, userMessage, LlmOptions.defaults());
    }

    /**
     * 호출 옵션을 지정해 응답을 생성한다. 요금제별로 모델·출력 길이를 달리할 때 쓴다.
     *
     * @param options 비어 있는 필드는 설정 파일의 기본값을 쓴다
     */
    LlmResponse complete(String systemPrompt, List<Turn> history, String userMessage, LlmOptions options);

    /** 실제 호출 없이 프롬프트의 입력 토큰 수만 센다. 쿼터 견적용 */
    int countTokens(String systemPrompt, List<Turn> history, String userMessage);

    /** 대화 한 턴 */
    record Turn(boolean fromUser, String content) {}

    /**
     * 호출 단위로 덮어쓸 옵션.
     *
     * <p>둘 다 null 을 허용한다. 요금제 하나가 바뀔 때마다 모델과 출력 길이를
     * 같이 넘겨야 하면 설정이 중복되므로, 지정하지 않은 값은 기본값으로 떨어뜨린다.
     *
     * @param model     사용할 모델 ID. null 이면 {@code app.llm.model}
     * @param maxTokens 최대 출력 토큰. null 이면 {@code app.llm.max-tokens}
     */
    record LlmOptions(String model, Long maxTokens) {

        private static final LlmOptions DEFAULTS = new LlmOptions(null, null);

        /** 설정 파일의 기본값을 그대로 쓴다 */
        public static LlmOptions defaults() {
            return DEFAULTS;
        }
    }

    /**
     * @param text         모델 응답 원문
     * @param inputTokens  입력 토큰
     * @param outputTokens 출력 토큰
     */
    record LlmResponse(String text, int inputTokens, int outputTokens) {}
}

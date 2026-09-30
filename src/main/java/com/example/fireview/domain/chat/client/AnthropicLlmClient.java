package com.example.fireview.domain.chat.client;

import com.anthropic.client.AnthropicClient;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCountTokensParams;
import com.anthropic.models.messages.MessageCreateParams;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Anthropic Messages API 기반 LlmClient 구현.
 *
 * Codyssey 게이트웨이는 /v1/messages 를 프로토콜 그대로 중계하므로
 * 공식 SDK 의 base-url 만 바꿔서 사용한다.
 */
@Slf4j
@Component
public class AnthropicLlmClient implements LlmClient {

    private final AnthropicClient client;
    private final String model;
    private final long maxTokens;

    public AnthropicLlmClient(AnthropicClient client,
                              @Value("${app.llm.model}") String model,
                              @Value("${app.llm.max-tokens:600}") long maxTokens) {
        this.client = client;
        this.model = model;
        this.maxTokens = maxTokens;
    }

    @Override
    public LlmResponse complete(String systemPrompt, List<Turn> history, String userMessage) {
        MessageCreateParams params = buildParams(systemPrompt, history, userMessage);
        Message response = client.messages().create(params);

        String text = response.content().stream()
                .flatMap(block -> block.text().stream())
                .map(textBlock -> textBlock.text())
                .reduce("", (a, b) -> a + b);

        int input = (int) response.usage().inputTokens();
        int output = (int) response.usage().outputTokens();
        log.info("[LLM] 응답 수신 - input={}, output={}, total={}", input, output, input + output);

        return new LlmResponse(text, input, output);
    }

    @Override
    public int countTokens(String systemPrompt, List<Turn> history, String userMessage) {
        MessageCountTokensParams.Builder builder = MessageCountTokensParams.builder()
                .model(model)
                .system(systemPrompt);
        appendTurns(history, userMessage,
                builder::addUserMessage,
                builder::addAssistantMessage);
        return (int) client.messages().countTokens(builder.build()).inputTokens();
    }

    private MessageCreateParams buildParams(String systemPrompt, List<Turn> history, String userMessage) {
        MessageCreateParams.Builder builder = MessageCreateParams.builder()
                .model(model)
                .maxTokens(maxTokens)
                .system(systemPrompt);
        appendTurns(history, userMessage,
                builder::addUserMessage,
                builder::addAssistantMessage);
        return builder.build();
    }

    /**
     * 대화 이력과 이번 질문을 순서대로 추가한다.
     * Messages API 는 user/assistant 가 번갈아 나와야 하므로 이력은 그대로 옮기고
     * 마지막에 이번 사용자 메시지를 붙인다.
     */
    private void appendTurns(List<Turn> history, String userMessage,
                             java.util.function.Consumer<String> addUser,
                             java.util.function.Consumer<String> addAssistant) {
        if (history != null) {
            for (Turn turn : history) {
                if (turn.fromUser()) {
                    addUser.accept(turn.content());
                } else {
                    addAssistant.accept(turn.content());
                }
            }
        }
        addUser.accept(userMessage);
    }
}

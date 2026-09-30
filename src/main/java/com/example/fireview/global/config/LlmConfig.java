package com.example.fireview.global.config;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * LLM 클라이언트 설정.
 *
 * base-url 을 Codyssey 게이트웨이로 돌려 공식 Anthropic SDK 를 그대로 쓴다.
 * 게이트웨이가 /v1/messages 를 프로토콜 그대로 중계하므로 SDK 코드는 바뀌지 않는다.
 */
@Slf4j
@Configuration
public class LlmConfig {

    @Bean
    public AnthropicClient anthropicClient(
            @Value("${app.llm.api-key:}") String apiKey,
            @Value("${app.llm.base-url:}") String baseUrl,
            @Value("${app.llm.timeout-seconds:60}") long timeoutSeconds) {

        if (apiKey.isBlank()) {
            log.warn("[LLM] app.llm.api-key 가 비어 있다. 챗봇 호출은 실패한다");
        }

        AnthropicOkHttpClient.Builder builder = AnthropicOkHttpClient.builder()
                .apiKey(apiKey)
                .timeout(Duration.ofSeconds(timeoutSeconds));

        if (!baseUrl.isBlank()) {
            builder.baseUrl(baseUrl);
            log.info("[LLM] 게이트웨이 사용 - baseUrl={}", baseUrl);
        }
        return builder.build();
    }
}

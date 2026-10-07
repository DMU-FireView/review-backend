package com.example.fireview.domain.chat.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class PromptAssemblerTest {

    private final PromptAssembler assembler = new PromptAssembler(new ObjectMapper(), new TopicGuard());

    @Test
    void 본문에_마크다운_기호를_쓰지_말라는_규칙이_있다() {
        String prompt = assembler.systemPrompt();

        assertThat(prompt).contains("[본문 서식]");
        assertThat(prompt).contains("마크다운 기호를 절대 쓰지 마라");
    }

    @Test
    void 프롬프트_자체도_마크다운_서식을_쓰지_않는다() {
        // 프롬프트가 제목·목록 서식을 쓰면 모델이 답변도 그 서식으로 쓴다.
        // 금지 기호를 설명하는 문장("별표(*, **)")은 서식이 아니므로, 굵게 표시로 감싼 경우만 본다.
        String[] lines = assembler.systemPrompt().split("\n");

        assertThat(Arrays.stream(lines).map(String::strip))
                .noneMatch(line -> line.startsWith("#"))
                .noneMatch(line -> line.startsWith("- "))
                .noneMatch(line -> line.startsWith("* "))
                .noneMatch(line -> line.matches(".*\\*\\*[^*\\s][^*]*\\*\\*.*"));
    }

    @Test
    void 응답_형식은_그대로_유지한다() {
        // LlmAnswer 가 첫 줄 ONTOPIC 과 구분 줄 --- 로 파싱한다
        String prompt = assembler.systemPrompt();

        assertThat(prompt).contains("ONTOPIC: yes", "ONTOPIC: no", "\n---\n");
    }
}

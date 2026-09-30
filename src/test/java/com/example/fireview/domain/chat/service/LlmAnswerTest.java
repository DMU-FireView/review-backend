package com.example.fireview.domain.chat.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LlmAnswerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void 정상_JSON을_파싱한다() {
        LlmAnswer parsed = LlmAnswer.parse(
                "{\"onTopic\": true, \"answer\": \"사이즈가 작다는 의견이 많습니다.\"}", objectMapper);

        assertThat(parsed.onTopic()).isTrue();
        assertThat(parsed.answer()).isEqualTo("사이즈가 작다는 의견이 많습니다.");
    }

    @Test
    void 코드펜스로_감싼_응답도_파싱한다() {
        String raw = """
                ```json
                {"onTopic": false, "answer": "상품과 리뷰에 대해서만 도와드릴 수 있어요."}
                ```""";
        LlmAnswer parsed = LlmAnswer.parse(raw, objectMapper);

        assertThat(parsed.onTopic()).isFalse();
        assertThat(parsed.answer()).contains("상품과 리뷰");
    }

    @Test
    void 앞뒤에_설명이_붙어도_JSON만_뽑아낸다() {
        String raw = "네, 답변드릴게요.\n{\"onTopic\": true, \"answer\": \"가격 대비 평이 좋습니다.\"}\n도움이 되셨길 바랍니다.";
        assertThat(LlmAnswer.parse(raw, objectMapper).answer()).isEqualTo("가격 대비 평이 좋습니다.");
    }

    @Test
    void 중괄호가_포함된_문자열이_있어도_올바른_범위를_잡는다() {
        String raw = "{\"onTopic\": true, \"answer\": \"리뷰에 {이런} 표현이 있습니다.\"}";
        assertThat(LlmAnswer.parse(raw, objectMapper).answer()).isEqualTo("리뷰에 {이런} 표현이 있습니다.");
    }

    @Test
    void JSON이_없으면_주제이탈로_처리한다() {
        LlmAnswer parsed = LlmAnswer.parse("그냥 평문 답변입니다.", objectMapper);

        // 형식을 어긴 응답을 그대로 노출하면 세이프가드를 우회하는 통로가 된다.
        assertThat(parsed.onTopic()).isFalse();
        assertThat(parsed.answer()).contains("다시 질문");
    }

    @Test
    void 깨진_JSON은_주제이탈로_처리한다() {
        assertThat(LlmAnswer.parse("{\"onTopic\": true, \"answer\":", objectMapper).onTopic()).isFalse();
    }

    @Test
    void answer가_비면_주제이탈로_처리한다() {
        assertThat(LlmAnswer.parse("{\"onTopic\": true, \"answer\": \"\"}", objectMapper).onTopic()).isFalse();
    }

    @Test
    void onTopic_필드가_없으면_false로_본다() {
        assertThat(LlmAnswer.parse("{\"answer\": \"답변\"}", objectMapper).onTopic()).isFalse();
    }

    @Test
    void null과_빈_입력을_안전하게_처리한다() {
        assertThat(LlmAnswer.parse(null, objectMapper).onTopic()).isFalse();
        assertThat(LlmAnswer.parse("", objectMapper).onTopic()).isFalse();
    }
}

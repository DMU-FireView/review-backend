package com.example.fireview.domain.chat.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LlmAnswerTest {

    @Test
    void 정상_형식을_파싱한다() {
        LlmAnswer parsed = LlmAnswer.parse("""
                ONTOPIC: yes
                ---
                사이즈가 작다는 의견이 많습니다.""");

        assertThat(parsed.onTopic()).isTrue();
        assertThat(parsed.answer()).isEqualTo("사이즈가 작다는 의견이 많습니다.");
    }

    @Test
    void 주제이탈_판정을_파싱한다() {
        LlmAnswer parsed = LlmAnswer.parse("""
                ONTOPIC: no
                ---
                상품과 리뷰에 대해서만 도와드릴 수 있어요.""");

        assertThat(parsed.onTopic()).isFalse();
        assertThat(parsed.answer()).contains("상품과 리뷰");
    }

    /**
     * JSON 형식을 버린 이유가 바로 이것이다.
     * 본문에 큰따옴표가 들어가도 파싱이 깨지지 않아야 한다.
     */
    @Test
    void 본문에_따옴표가_있어도_파싱한다() {
        LlmAnswer parsed = LlmAnswer.parse("""
                ONTOPIC: yes
                ---
                리뷰에 "사이즈가 작다"는 표현이 반복됩니다. 'S' 사이즈는 피하세요.""");

        assertThat(parsed.onTopic()).isTrue();
        assertThat(parsed.answer()).contains("\"사이즈가 작다\"");
    }

    @Test
    void 본문의_줄바꿈과_마크다운을_보존한다() {
        LlmAnswer parsed = LlmAnswer.parse("""
                ONTOPIC: yes
                ---
                **장점**
                - 가격 대비 품질

                **단점**
                - 사이즈가 작음""");

        assertThat(parsed.answer()).contains("**장점**").contains("\n").endsWith("사이즈가 작음");
    }

    @Test
    void 본문에_구분자가_또_나와도_첫_구분자만_기준으로_한다() {
        LlmAnswer parsed = LlmAnswer.parse("""
                ONTOPIC: yes
                ---
                장점은 이렇습니다.
                ---
                단점은 이렇습니다.""");

        assertThat(parsed.answer()).startsWith("장점은").contains("---").contains("단점은");
    }

    @Test
    void 코드펜스로_감싼_응답도_파싱한다() {
        LlmAnswer parsed = LlmAnswer.parse("""
                ```
                ONTOPIC: yes
                ---
                가격 대비 평이 좋습니다.
                ```""");

        assertThat(parsed.onTopic()).isTrue();
        assertThat(parsed.answer()).isEqualTo("가격 대비 평이 좋습니다.");
    }

    @Test
    void 구분자를_빠뜨려도_본문을_건진다() {
        LlmAnswer parsed = LlmAnswer.parse("""
                ONTOPIC: yes
                가격 대비 평이 좋습니다.""");

        assertThat(parsed.onTopic()).isTrue();
        assertThat(parsed.answer()).isEqualTo("가격 대비 평이 좋습니다.");
    }

    @Test
    void true_false_표기도_허용한다() {
        assertThat(LlmAnswer.parse("ONTOPIC: true\n---\n답변").onTopic()).isTrue();
        assertThat(LlmAnswer.parse("ONTOPIC: false\n---\n답변").onTopic()).isFalse();
    }

    @Test
    void 헤더가_없으면_주제이탈로_처리한다() {
        // 형식을 어긴 응답을 그대로 노출하면 세이프가드 3계층을 우회하는 통로가 된다
        LlmAnswer parsed = LlmAnswer.parse("그냥 평문 답변입니다.");

        assertThat(parsed.onTopic()).isFalse();
        assertThat(parsed.answer()).contains("다시 질문");
    }

    @Test
    void 본문이_비면_주제이탈로_처리한다() {
        assertThat(LlmAnswer.parse("ONTOPIC: yes\n---\n   ").onTopic()).isFalse();
    }

    @Test
    void null과_빈_입력을_안전하게_처리한다() {
        assertThat(LlmAnswer.parse(null).onTopic()).isFalse();
        assertThat(LlmAnswer.parse("").onTopic()).isFalse();
    }
}

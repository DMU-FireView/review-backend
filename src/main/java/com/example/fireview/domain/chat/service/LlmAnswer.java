package com.example.fireview.domain.chat.service;

import lombok.extern.slf4j.Slf4j;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * LLM 이 돌려준 구조화 응답.
 *
 * 시스템 프롬프트가 아래 형식을 요구한다.
 * <pre>
 * ONTOPIC: yes
 * ---
 * 답변 본문
 * </pre>
 *
 * <p><b>JSON 이 아니라 구분자를 쓰는 이유</b><br>
 * JSON 으로 받으면 모델이 본문에 큰따옴표를 이스케이프 없이 넣는 순간 파싱이 깨진다.
 * 실제 게이트웨이 호출에서 간헐적으로 재현됐고, 실패할 때마다 호출 1회분 토큰이 버려진다.
 * 이스케이프 규칙을 더 강하게 지시하는 방식은 신뢰할 수 없어서,
 * 이스케이프 자체가 필요 없는 형식으로 바꿨다. 본문에 따옴표·줄바꿈·마크다운이 와도 안전하다.
 *
 * <p>형식을 벗어난 응답은 주제 판정을 통과시키지 않는다(fail-closed).
 * 형식을 어긴 응답을 그대로 노출하면 세이프가드 3계층을 우회하는 통로가 되기 때문이다.
 */
@Slf4j
public record LlmAnswer(boolean onTopic, String answer) {

    /** 모델이 형식을 어겼을 때 사용자에게 보여줄 문구 */
    private static final String MALFORMED_FALLBACK =
            "답변을 정리하지 못했습니다. 상품이나 리뷰에 대해 다시 질문해주세요.";

    private static final Pattern HEADER =
            Pattern.compile("^\\s*ONTOPIC\\s*[:：]\\s*(yes|no|true|false)\\s*$",
                    Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);

    /** 헤더 다음에 오는 구분자 줄 */
    private static final Pattern SEPARATOR = Pattern.compile("^\\s*-{3,}\\s*$", Pattern.MULTILINE);

    public static LlmAnswer parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return malformed();
        }
        String text = stripCodeFence(raw);

        Matcher header = HEADER.matcher(text);
        if (!header.find()) {
            log.warn("[Chat] ONTOPIC 헤더가 없는 응답. 길이={}", text.length());
            return malformed();
        }
        String flag = header.group(1).toLowerCase();
        boolean onTopic = flag.equals("yes") || flag.equals("true");

        String body = extractBody(text, header.end());
        if (body.isBlank()) {
            log.warn("[Chat] 본문이 비어 있는 응답");
            return malformed();
        }
        return new LlmAnswer(onTopic, body);
    }

    private static LlmAnswer malformed() {
        return new LlmAnswer(false, MALFORMED_FALLBACK);
    }

    /**
     * 헤더 뒤의 본문을 뽑는다.
     * 구분자가 있으면 그 다음부터, 없으면 헤더 다음 줄부터를 본문으로 본다.
     * 모델이 구분자를 빠뜨리는 경우가 형식 위반 중 가장 흔하고 피해도 없어서 허용한다.
     */
    private static String extractBody(String text, int headerEnd) {
        String rest = text.substring(headerEnd);
        Matcher sep = SEPARATOR.matcher(rest);
        String body = sep.find() ? rest.substring(sep.end()) : rest;
        return body.strip();
    }

    /** ```로 감싼 응답에서 펜스를 걷어낸다. */
    private static String stripCodeFence(String raw) {
        String text = raw.strip();
        if (!text.startsWith("```")) {
            return text;
        }
        int firstLineEnd = text.indexOf('\n');
        if (firstLineEnd < 0) {
            return text;
        }
        String inner = text.substring(firstLineEnd + 1);
        int closing = inner.lastIndexOf("```");
        return (closing >= 0 ? inner.substring(0, closing) : inner).strip();
    }
}

package com.example.fireview.domain.chat.dto.request;

import com.example.fireview.domain.chat.service.TopicGuard;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * @param sessionId 이어갈 대화. null 이면 새 대화를 시작한다
 * @param productId 대화 대상 상품 (새 대화일 때만 반영)
 * @param question  사용자 질문
 */
public record ChatRequest(

        Long sessionId,

        @Size(max = 100)
        String productId,

        @NotBlank(message = "질문을 입력해주세요.")
        @Size(max = TopicGuard.MAX_QUESTION_LENGTH,
                message = "질문은 " + TopicGuard.MAX_QUESTION_LENGTH + "자 이내로 입력해주세요.")
        String question
) {}

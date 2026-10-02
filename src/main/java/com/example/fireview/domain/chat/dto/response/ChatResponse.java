package com.example.fireview.domain.chat.dto.response;

import com.example.fireview.domain.chat.service.ChatService;

/**
 * @param sessionId   대화 세션 ID. 다음 질문에 그대로 넣으면 대화가 이어진다
 * @param answer      챗봇 답변 (차단된 경우 안내 문구)
 * @param blocked     세이프가드에 걸렸는지
 * @param blockReason 차단 사유 코드 (INJECTION / OFF_TOPIC / UNGROUNDED_SCORE 등)
 * @param usedTokens  이번 턴에 소모한 토큰
 * @param quota       이 턴을 반영한 오늘 사용량. 남은 횟수 표시에 쓴다
 */
public record ChatResponse(
        Long sessionId,
        String answer,
        boolean blocked,
        String blockReason,
        int usedTokens,
        ChatQuotaResponse quota
) {
    public static ChatResponse from(ChatService.ChatResult result) {
        return new ChatResponse(result.sessionId(), result.answer(),
                result.blocked(), result.blockReason(), result.usedTokens(),
                ChatQuotaResponse.from(result.quota()));
    }
}

package com.example.fireview.domain.chat.dto.response;

import com.example.fireview.domain.chat.service.ChatService;
import com.example.fireview.domain.user.entity.PlanTier;

import java.time.Instant;

/**
 * 오늘 남은 챗봇 사용량.
 *
 * @param plan         적용 중인 요금제 (만료된 유료 요금제는 FREE 로 내려온다)
 * @param planName     요금제 표시명 (무료 / 플러스 / 프로)
 * @param dailyLimit   하루 한도. {@code -1} 이면 무제한
 * @param usedToday    오늘 사용한 메시지 수
 * @param remaining    남은 메시지 수. {@code -1} 이면 무제한
 * @param proAvailable {@code POST /api/chat/pro/messages} 를 쓸 수 있는지
 * @param resetAt      한도가 초기화되는 시각 (Asia/Seoul 자정)
 */
public record ChatQuotaResponse(
        PlanTier plan,
        String planName,
        int dailyLimit,
        int usedToday,
        int remaining,
        boolean proAvailable,
        Instant resetAt
) {
    public static ChatQuotaResponse from(ChatService.QuotaStatus status) {
        return new ChatQuotaResponse(
                status.plan(),
                status.plan().getDisplayName(),
                status.dailyLimit(),
                status.usedToday(),
                status.remaining(),
                status.proAvailable(),
                status.resetAt());
    }
}

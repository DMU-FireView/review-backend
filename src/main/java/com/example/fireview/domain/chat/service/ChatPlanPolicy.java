package com.example.fireview.domain.chat.service;

import com.example.fireview.domain.chat.client.LlmClient;
import com.example.fireview.domain.chat.entity.ChatTier;
import com.example.fireview.domain.user.entity.PlanTier;
import com.example.fireview.global.exception.CustomException;
import com.example.fireview.global.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 요금제별 정책을 한 곳에 모은 클래스.
 *
 * <p>"요금제마다 무엇이 다른가"가 서비스·컨트롤러·설정 파일에 흩어지면
 * 가격을 바꿀 때 빠뜨리는 곳이 생긴다. 하루 한도, 쓸 수 있는 엔드포인트,
 * LLM 호출 옵션을 모두 여기서 결정한다.
 *
 * <p>하루 한도는 코드가 아니라 설정값이다. 사업상 수시로 바뀌는 숫자를
 * enum 에 박아두면 바꿀 때마다 배포가 필요하다.
 */
@Slf4j
@Component
public class ChatPlanPolicy {

    private final Map<PlanTier, Integer> dailyLimits;
    private final String proModel;
    private final long proMaxTokens;

    public ChatPlanPolicy(
            @Value("${app.chat.quota.free:5}") int freeLimit,
            @Value("${app.chat.quota.plus:100}") int plusLimit,
            @Value("${app.chat.quota.pro:300}") int proLimit,
            @Value("${app.llm.pro.model:}") String proModel,
            @Value("${app.llm.pro.max-tokens:1200}") long proMaxTokens) {

        this.dailyLimits = Map.of(
                PlanTier.FREE, freeLimit,
                PlanTier.PLUS, plusLimit,
                PlanTier.PRO, proLimit);
        this.proModel = proModel;
        this.proMaxTokens = proMaxTokens;

        log.info("[ChatPlan] 하루 한도 - FREE={}, PLUS={}, PRO={}", freeLimit, plusLimit, proLimit);
    }

    /** 하루 메시지 한도. {@link ChatQuotaStore#UNLIMITED} 면 무제한 */
    public int dailyLimit(PlanTier plan) {
        return dailyLimits.getOrDefault(plan == null ? PlanTier.FREE : plan,
                dailyLimits.get(PlanTier.FREE));
    }

    /** 해당 요금제가 쓸 수 있는 엔드포인트인지 */
    public boolean canUse(PlanTier plan, ChatTier tier) {
        if (tier == ChatTier.STANDARD) return true;
        return plan == PlanTier.PRO;
    }

    /**
     * 엔드포인트 접근 권한을 검사한다. 못 쓰면 403 으로 끊는다.
     *
     * <p>이 검사를 SecurityConfig 에 두지 않는 이유: 요금제는 JWT 클레임이 아니라
     * DB 값이다. 결제 직후 재로그인 없이 바로 반영되어야 하므로 매 요청마다 DB 를 본다.
     */
    public void verifyAccess(PlanTier plan, ChatTier tier) {
        if (!canUse(plan, tier)) {
            throw new CustomException(ErrorCode.CHAT_PLAN_REQUIRED);
        }
    }

    /**
     * 엔드포인트 등급별 LLM 호출 옵션.
     *
     * <p>STANDARD 는 기본 설정을 그대로 쓰고, PRO 만 모델·출력 길이를 올린다.
     * {@code app.llm.pro.model} 이 비어 있으면 모델은 기본값을 쓰고 출력 길이만 늘어난다.
     */
    public LlmClient.LlmOptions llmOptions(ChatTier tier) {
        if (tier != ChatTier.PRO) {
            return LlmClient.LlmOptions.defaults();
        }
        return new LlmClient.LlmOptions(proModel.isBlank() ? null : proModel, proMaxTokens);
    }
}

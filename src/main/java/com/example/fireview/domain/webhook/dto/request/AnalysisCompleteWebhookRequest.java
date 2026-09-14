package com.example.fireview.domain.webhook.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Data 서버가 분석 job 종료 시 호출하는 완료 웹훅의 본문.
 * 규격은 docs/webhook-contract.md 참고.
 */
public record AnalysisCompleteWebhookRequest(

        /** Data 서버가 발급한 job 식별자. 멱등성 키로 사용된다. */
        @NotBlank(message = "jobId는 필수입니다.")
        @Size(max = 100)
        String jobId,

        /** 분석 대상 상품의 외부 식별자 (Data 서버 기준) */
        @NotBlank(message = "productId는 필수입니다.")
        @Size(max = 100)
        String productId,

        /** 알림에 표시할 상품명 (선택) */
        @Size(max = 200)
        String productName,

        /** 분석을 요청한 사용자 이메일 — 알림 수신자 */
        @NotBlank(message = "requesterEmail은 필수입니다.")
        @Email
        String requesterEmail,

        @NotNull(message = "status는 필수입니다.")
        Status status,

        /** 실패 시 사유 (선택) */
        @Size(max = 500)
        String failureReason
) {
    public enum Status { COMPLETED, FAILED }

    public boolean isCompleted() {
        return status == Status.COMPLETED;
    }
}

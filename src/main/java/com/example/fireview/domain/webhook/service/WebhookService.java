package com.example.fireview.domain.webhook.service;

import com.example.fireview.domain.notification.entity.NotificationType;
import com.example.fireview.domain.notification.service.NotificationService;
import com.example.fireview.domain.user.entity.User;
import com.example.fireview.domain.user.repository.UserRepository;
import com.example.fireview.domain.webhook.dto.request.AnalysisCompleteWebhookRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Data 서버에서 오는 웹훅을 처리한다.
 *
 * 웹훅은 중복 전송과 유실이 모두 정상 범위이므로:
 * - 같은 jobId 는 한 번만 처리한다 (ProcessedWebhookStore)
 * - 처리 중 예외가 나면 표시를 되돌려 Data 서버의 재전송이 다시 처리되게 한다
 * - 수신자를 찾지 못하는 경우는 재전송해도 해결되지 않으므로 조용히 무시한다
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookService {

    private final ProcessedWebhookStore processedWebhookStore;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public void handleAnalysisComplete(AnalysisCompleteWebhookRequest request) {
        String jobId = request.jobId();

        if (!processedWebhookStore.markIfAbsent(jobId)) {
            log.info("[Webhook] 중복 웹훅 무시 - jobId={}", jobId);
            return;
        }

        try {
            Optional<User> receiver = userRepository.findByEmail(request.requesterEmail());
            if (receiver.isEmpty()) {
                log.warn("[Webhook] 수신자 없음 - jobId={}, email={}", jobId, request.requesterEmail());
                return;
            }
            notify(receiver.get(), request);
            log.info("[Webhook] 분석 {} 알림 발송 - jobId={}, productId={}",
                    request.isCompleted() ? "완료" : "실패", jobId, request.productId());
        } catch (RuntimeException e) {
            processedWebhookStore.release(jobId);
            throw e;
        }
    }

    private void notify(User receiver, AnalysisCompleteWebhookRequest request) {
        String productLabel = request.productName() != null ? "'" + request.productName() + "' " : "";
        String targetUrl = "/products/" + request.productId();

        if (request.isCompleted()) {
            notificationService.createNotification(
                    receiver,
                    NotificationType.ANALYSIS_COMPLETE,
                    "AI 분석 완료",
                    productLabel + "상품의 리뷰 분석이 완료되었습니다.",
                    targetUrl);
            return;
        }

        String reason = request.failureReason() != null ? " (" + request.failureReason() + ")" : "";
        notificationService.createNotification(
                receiver,
                NotificationType.ANALYSIS_FAILED,
                "AI 분석 실패",
                productLabel + "상품의 리뷰 분석에 실패했습니다" + reason + ". 잠시 후 다시 시도해주세요.",
                targetUrl);
    }
}

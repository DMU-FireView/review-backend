package com.example.fireview.domain.webhook.service;

import com.example.fireview.domain.notification.entity.NotificationType;
import com.example.fireview.domain.notification.service.NotificationService;
import com.example.fireview.domain.user.entity.User;
import com.example.fireview.domain.user.repository.UserRepository;
import com.example.fireview.domain.webhook.dto.request.AnalysisCompleteWebhookRequest;
import com.example.fireview.domain.webhook.dto.request.AnalysisCompleteWebhookRequest.Status;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WebhookServiceTest {

    @Mock UserRepository userRepository;
    @Mock NotificationService notificationService;

    private ProcessedWebhookStore store;
    private WebhookService service;
    private User receiver;

    @BeforeEach
    void setUp() {
        store = new ProcessedWebhookStore(); // 실제 인메모리 구현으로 멱등성까지 검증
        service = new WebhookService(store, userRepository, notificationService);
        receiver = User.builder().email("user@test.com").nickname("tester").build();
    }

    @Test
    void 완료_웹훅이면_ANALYSIS_COMPLETE_알림을_만든다() {
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(receiver));

        service.handleAnalysisComplete(request("job-1", Status.COMPLETED, null));

        verify(notificationService).createNotification(
                eq(receiver), eq(NotificationType.ANALYSIS_COMPLETE),
                eq("AI 분석 완료"), contains("완료되었습니다"), eq("/products/p-1"));
    }

    @Test
    void 실패_웹훅이면_ANALYSIS_FAILED_알림에_사유를_담는다() {
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(receiver));

        service.handleAnalysisComplete(request("job-2", Status.FAILED, "크롤링 차단"));

        verify(notificationService).createNotification(
                eq(receiver), eq(NotificationType.ANALYSIS_FAILED),
                eq("AI 분석 실패"), contains("크롤링 차단"), eq("/products/p-1"));
    }

    @Test
    void 같은_jobId가_두_번_오면_알림은_한_번만_만든다() {
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(receiver));
        AnalysisCompleteWebhookRequest request = request("job-3", Status.COMPLETED, null);

        service.handleAnalysisComplete(request);
        service.handleAnalysisComplete(request);

        verify(notificationService, times(1))
                .createNotification(any(), any(), anyString(), anyString(), anyString());
    }

    @Test
    void 수신자를_찾지_못하면_예외_없이_알림을_건너뛴다() {
        when(userRepository.findByEmail("ghost@test.com")).thenReturn(Optional.empty());

        service.handleAnalysisComplete(new AnalysisCompleteWebhookRequest(
                "job-4", "p-1", null, "ghost@test.com", Status.COMPLETED, null));

        verify(notificationService, never())
                .createNotification(any(), any(), anyString(), anyString(), anyString());
    }

    @Test
    void 처리_중_예외가_나면_표시를_되돌려_재전송이_다시_처리되게_한다() {
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(receiver));
        doThrow(new RuntimeException("DB down"))
                .doNothing()
                .when(notificationService)
                .createNotification(any(), any(), anyString(), anyString(), anyString());
        AnalysisCompleteWebhookRequest request = request("job-5", Status.COMPLETED, null);

        assertThatThrownBy(() -> service.handleAnalysisComplete(request))
                .isInstanceOf(RuntimeException.class);

        // 재전송: 첫 시도에서 release 됐으므로 중복으로 취급되지 않고 다시 처리된다
        service.handleAnalysisComplete(request);

        verify(notificationService, times(2))
                .createNotification(any(), any(), anyString(), anyString(), anyString());
    }

    private static AnalysisCompleteWebhookRequest request(String jobId, Status status, String failureReason) {
        return new AnalysisCompleteWebhookRequest(
                jobId, "p-1", "테스트 상품", "user@test.com", status, failureReason);
    }
}

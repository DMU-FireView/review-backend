package com.example.fireview.domain.notification.entity;

import com.example.fireview.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 알림 엔티티
 *
 * - receiver: 알림 수신자
 * - type: 알림 유형 (신고 접수/처리, 시스템 등)
 * - title: 알림 제목 (예: "신고가 접수되었습니다")
 * - message: 알림 본문 (예: "리뷰 '좋은 제품...' 신고가 접수되었습니다.")
 * - isRead: 읽음 여부
 * - targetUrl: 알림 클릭 시 이동 경로 (예: /reports/me/1)
 */
@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id", nullable = false)
    private User receiver;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, length = 500)
    private String message;

    @Column(nullable = false)
    @Builder.Default
    private boolean isRead = false;

    private String targetUrl;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public void markAsRead() {
        this.isRead = true;
    }
}

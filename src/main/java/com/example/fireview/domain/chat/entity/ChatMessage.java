package com.example.fireview.domain.chat.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 챗봇 대화의 개별 메시지.
 *
 * 세이프가드에 걸려 거절된 질문도 저장한다(blocked=true).
 * 어떤 질문이 왜 막혔는지가 프롬프트 튜닝의 근거가 되기 때문이다.
 */
@Entity
@Table(name = "chat_messages", indexes = @Index(name = "idx_chat_message_session", columnList = "session_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private ChatSession session;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ChatRole role;

    @Column(nullable = false, length = 4000)
    private String content;

    /** 세이프가드에 걸린 메시지 여부 */
    @Column(nullable = false)
    @Builder.Default
    private boolean blocked = false;

    /** 차단 사유 (blocked=true 일 때만) */
    @Column(length = 100)
    private String blockReason;

    /** 이 메시지를 만드는 데 쓴 토큰 수 (ASSISTANT 만). 쿼터 소진 추적용 */
    private Integer inputTokens;
    private Integer outputTokens;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}

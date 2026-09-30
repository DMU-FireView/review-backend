package com.example.fireview.domain.chat.entity;

import com.example.fireview.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 챗봇 대화 세션.
 *
 * 하나의 세션은 하나의 상품에 대한 대화를 담는다.
 * productId 는 Data 서버가 소유하는 외부 식별자이므로 FK 가 아니라 문자열로만 보관한다.
 */
@Entity
@Table(name = "chat_sessions", indexes = @Index(name = "idx_chat_session_user", columnList = "user_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** 대화 대상 상품의 외부 식별자 (Data 서버 기준). 상품 무관 대화면 null */
    @Column(length = 100)
    private String productId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime lastMessageAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        if (lastMessageAt == null) lastMessageAt = now;
    }

    public void touch() {
        this.lastMessageAt = LocalDateTime.now();
    }

    public boolean isOwnedBy(User other) {
        return user != null && other != null && user.getId().equals(other.getId());
    }
}

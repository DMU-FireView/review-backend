package com.example.fireview.domain.chat.dto.response;

import com.example.fireview.domain.chat.entity.ChatSession;

import java.time.LocalDateTime;

public record ChatSessionResponse(
        Long id,
        String productId,
        String title,
        LocalDateTime createdAt,
        LocalDateTime lastMessageAt
) {
    public static ChatSessionResponse from(ChatSession session) {
        return new ChatSessionResponse(
                session.getId(), session.getProductId(), session.getTitle(),
                session.getCreatedAt(), session.getLastMessageAt());
    }
}

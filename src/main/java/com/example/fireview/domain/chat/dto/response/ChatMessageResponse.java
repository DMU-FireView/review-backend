package com.example.fireview.domain.chat.dto.response;

import com.example.fireview.domain.chat.entity.ChatMessage;
import com.example.fireview.domain.chat.entity.ChatRole;

import java.time.LocalDateTime;

public record ChatMessageResponse(
        Long id,
        ChatRole role,
        String content,
        boolean blocked,
        String blockReason,
        LocalDateTime createdAt
) {
    public static ChatMessageResponse from(ChatMessage message) {
        return new ChatMessageResponse(
                message.getId(), message.getRole(), message.getContent(),
                message.isBlocked(), message.getBlockReason(), message.getCreatedAt());
    }
}

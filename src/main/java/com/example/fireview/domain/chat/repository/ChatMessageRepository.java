package com.example.fireview.domain.chat.repository;

import com.example.fireview.domain.chat.entity.ChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findBySession_IdOrderByCreatedAtAsc(Long sessionId);

    /** 프롬프트에 실을 최근 대화만 가져온다. 이력 전체를 보내면 토큰이 선형으로 늘어난다. */
    List<ChatMessage> findBySession_IdAndBlockedFalseOrderByCreatedAtDesc(Long sessionId, Pageable pageable);
}

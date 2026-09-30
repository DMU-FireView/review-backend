package com.example.fireview.domain.chat.repository;

import com.example.fireview.domain.chat.entity.ChatSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {

    Page<ChatSession> findByUser_IdOrderByLastMessageAtDesc(Long userId, Pageable pageable);
}

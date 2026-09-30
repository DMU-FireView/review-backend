package com.example.fireview.domain.chat.service;

import com.example.fireview.domain.chat.client.LlmClient;
import com.example.fireview.domain.chat.entity.ChatMessage;
import com.example.fireview.domain.chat.entity.ChatRole;
import com.example.fireview.domain.chat.entity.ChatSession;
import com.example.fireview.domain.chat.port.ProductAnalysisContext;
import com.example.fireview.domain.chat.port.ProductAnalysisPort;
import com.example.fireview.domain.chat.repository.ChatMessageRepository;
import com.example.fireview.domain.chat.repository.ChatSessionRepository;
import com.example.fireview.domain.user.entity.User;
import com.example.fireview.domain.user.service.UserService;
import com.example.fireview.global.exception.CustomException;
import com.example.fireview.global.exception.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 챗봇 대화 오케스트레이션.
 *
 * 흐름: 세이프가드 1계층 → 컨텍스트 조회 → 프롬프트 조립 → LLM 호출
 *      → 구조화 응답 파싱(3계층) → 세이프가드 4계층 → 저장
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatSessionRepository sessionRepository;
    private final ChatMessageRepository messageRepository;
    private final ProductAnalysisPort productAnalysisPort;
    private final PromptAssembler promptAssembler;
    private final TopicGuard topicGuard;
    private final LlmClient llmClient;
    private final UserService userService;
    private final ObjectMapper objectMapper;

    /**
     * @param sessionId 이어갈 세션. null 이면 새로 만든다
     * @param productId 대화 대상 상품 (새 세션일 때만 사용)
     */
    @Transactional
    public ChatResult ask(String userEmail, Long sessionId, String productId, String question) {
        User user = userService.findByEmail(userEmail);
        ChatSession session = resolveSession(user, sessionId, productId, question);

        // ── 세이프가드 1계층: 입력 검증 (여기서 막히면 토큰 소모 0) ──
        TopicGuard.Verdict input = topicGuard.inspectQuestion(question);
        if (!input.allowed()) {
            return blockAndSave(session, question, input.reason(), input.userMessage());
        }

        ProductAnalysisContext context = session.getProductId() == null ? null
                : productAnalysisPort.findContext(session.getProductId()).orElse(null);

        String systemPrompt = promptAssembler.systemPrompt();
        String userMessage = promptAssembler.buildUserMessage(context, question);
        List<LlmClient.Turn> history = loadHistory(session);

        LlmClient.LlmResponse response;
        try {
            response = llmClient.complete(systemPrompt, history, userMessage);
        } catch (RuntimeException e) {
            log.error("[Chat] LLM 호출 실패 - sessionId={}: {}", session.getId(), e.getMessage());
            throw new CustomException(ErrorCode.CHAT_LLM_UNAVAILABLE);
        }

        // ── 세이프가드 3계층: 모델의 주제 판정 ──
        LlmAnswer parsed = LlmAnswer.parse(response.text(), objectMapper);
        if (!parsed.onTopic()) {
            log.info("[Chat] 주제 이탈 응답 - sessionId={}", session.getId());
            return blockAndSave(session, question, "OFF_TOPIC", parsed.answer(),
                    response.inputTokens(), response.outputTokens());
        }

        // ── 세이프가드 4계층: 근거 없는 수치 차단 ──
        TopicGuard.Verdict output = topicGuard.inspectAnswer(parsed.answer(), context);
        if (!output.allowed()) {
            return blockAndSave(session, question, output.reason(), output.userMessage(),
                    response.inputTokens(), response.outputTokens());
        }

        saveTurn(session, question, parsed.answer(), false, null,
                response.inputTokens(), response.outputTokens());
        session.touch();

        return new ChatResult(session.getId(), parsed.answer(), false, null,
                response.inputTokens() + response.outputTokens());
    }

    @Transactional(readOnly = true)
    public List<ChatMessage> getMessages(String userEmail, Long sessionId) {
        User user = userService.findByEmail(userEmail);
        ChatSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHAT_SESSION_NOT_FOUND));
        if (!session.isOwnedBy(user)) {
            throw new CustomException(ErrorCode.CHAT_SESSION_FORBIDDEN);
        }
        return messageRepository.findBySession_IdOrderByCreatedAtAsc(sessionId);
    }

    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<ChatSession> getMySessions(
            String userEmail, org.springframework.data.domain.Pageable pageable) {
        User user = userService.findByEmail(userEmail);
        return sessionRepository.findByUser_IdOrderByLastMessageAtDesc(user.getId(), pageable);
    }

    // ────────────────────────────── 내부 ──────────────────────────────

    private ChatSession resolveSession(User user, Long sessionId, String productId, String question) {
        if (sessionId == null) {
            return sessionRepository.save(ChatSession.builder()
                    .user(user)
                    .productId(productId)
                    .title(deriveTitle(question))
                    .build());
        }
        ChatSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHAT_SESSION_NOT_FOUND));
        if (!session.isOwnedBy(user)) {
            throw new CustomException(ErrorCode.CHAT_SESSION_FORBIDDEN);
        }
        return session;
    }

    /** 프롬프트에 실을 최근 대화만 오래된 순으로 만든다. 이력 전체를 보내면 토큰이 선형으로 늘어난다. */
    private List<LlmClient.Turn> loadHistory(ChatSession session) {
        if (session.getId() == null) return List.of();

        List<ChatMessage> recent = new ArrayList<>(messageRepository
                .findBySession_IdAndBlockedFalseOrderByCreatedAtDesc(
                        session.getId(), PageRequest.of(0, PromptAssembler.MAX_HISTORY_MESSAGES)));
        recent.sort(Comparator.comparing(ChatMessage::getCreatedAt));

        return recent.stream()
                .map(m -> new LlmClient.Turn(m.getRole() == ChatRole.USER, m.getContent()))
                .toList();
    }

    private ChatResult blockAndSave(ChatSession session, String question, String reason, String userMessage) {
        return blockAndSave(session, question, reason, userMessage, null, null);
    }

    private ChatResult blockAndSave(ChatSession session, String question, String reason,
                                    String userMessage, Integer inputTokens, Integer outputTokens) {
        saveTurn(session, question, userMessage, true, reason, inputTokens, outputTokens);
        session.touch();
        int used = (inputTokens == null ? 0 : inputTokens) + (outputTokens == null ? 0 : outputTokens);
        return new ChatResult(session.getId(), userMessage, true, reason, used);
    }

    private void saveTurn(ChatSession session, String question, String answer,
                          boolean blocked, String blockReason,
                          Integer inputTokens, Integer outputTokens) {
        messageRepository.save(ChatMessage.builder()
                .session(session)
                .role(ChatRole.USER)
                .content(truncate(question))
                .blocked(blocked)
                .blockReason(blockReason)
                .build());
        messageRepository.save(ChatMessage.builder()
                .session(session)
                .role(ChatRole.ASSISTANT)
                .content(truncate(answer))
                .blocked(blocked)
                .blockReason(blockReason)
                .inputTokens(inputTokens)
                .outputTokens(outputTokens)
                .build());
    }

    private static String deriveTitle(String question) {
        if (question == null || question.isBlank()) return "새 대화";
        String trimmed = question.strip();
        return trimmed.length() <= 30 ? trimmed : trimmed.substring(0, 30) + "...";
    }

    private static String truncate(String text) {
        if (text == null) return "";
        return text.length() <= 4000 ? text : text.substring(0, 4000);
    }

    /**
     * @param blocked     세이프가드에 걸렸는지
     * @param blockReason 차단 사유 (blocked=true 일 때)
     * @param usedTokens  이번 턴에 소모한 토큰 (쿼터 추적용)
     */
    public record ChatResult(Long sessionId, String answer, boolean blocked,
                             String blockReason, int usedTokens) {}
}

package com.example.fireview.domain.chat.controller;

import com.example.fireview.domain.chat.dto.request.ChatRequest;
import com.example.fireview.domain.chat.dto.response.ChatMessageResponse;
import com.example.fireview.domain.chat.dto.response.ChatResponse;
import com.example.fireview.domain.chat.dto.response.ChatSessionResponse;
import com.example.fireview.domain.chat.service.ChatService;
import com.example.fireview.global.config.ExecutorConfig;
import com.example.fireview.global.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;

import java.util.List;
import java.util.concurrent.Executor;

/**
 * 챗봇 API.
 *
 * 질문 전송은 DeferredResult 로 처리해 LLM 응답을 기다리는 동안
 * Tomcat 워커 스레드를 붙잡지 않는다. 실제 대기는 챗봇 전용 풀에서 일어나므로
 * 챗봇이 느려져도 로그인·찜 같은 일반 API 가 밀리지 않는다.
 */
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;
    private final Executor chatExecutor;

    // Executor 빈이 둘(aiCallExecutor, chatExecutor)이라 @Qualifier 가 필요하다.
    // Lombok 의 @RequiredArgsConstructor 는 필드의 @Qualifier 를 생성자 파라미터로
    // 옮겨주지 않으므로 생성자를 직접 쓴다.
    public ChatController(ChatService chatService,
                          @Qualifier(ExecutorConfig.CHAT_EXECUTOR) Executor chatExecutor) {
        this.chatService = chatService;
        this.chatExecutor = chatExecutor;
    }

    /**
     * 질문 전송
     * POST /api/chat/messages
     */
    @PostMapping("/messages")
    public DeferredResult<ApiResponse<ChatResponse>> ask(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ChatRequest request) {

        DeferredResult<ApiResponse<ChatResponse>> deferred = new DeferredResult<>(70_000L);

        chatExecutor.execute(() -> {
            try {
                ChatService.ChatResult result = chatService.ask(
                        jwt.getSubject(), request.sessionId(), request.productId(), request.question());
                deferred.setResult(ApiResponse.success(ChatResponse.from(result)));
            } catch (Exception e) {
                deferred.setErrorResult(e);
            }
        });
        return deferred;
    }

    /**
     * 내 대화 목록
     * GET /api/chat/sessions
     */
    @GetMapping("/sessions")
    public ApiResponse<Page<ChatSessionResponse>> getMySessions(
            @AuthenticationPrincipal Jwt jwt,
            @PageableDefault(size = 20, sort = "lastMessageAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        Page<ChatSessionResponse> sessions =
                chatService.getMySessions(jwt.getSubject(), pageable).map(ChatSessionResponse::from);
        return ApiResponse.success(sessions);
    }

    /**
     * 대화 내용 조회
     * GET /api/chat/sessions/{sessionId}/messages
     */
    @GetMapping("/sessions/{sessionId}/messages")
    public ApiResponse<List<ChatMessageResponse>> getMessages(
            @PathVariable Long sessionId,
            @AuthenticationPrincipal Jwt jwt) {
        List<ChatMessageResponse> messages = chatService.getMessages(jwt.getSubject(), sessionId)
                .stream().map(ChatMessageResponse::from).toList();
        return ApiResponse.success(messages);
    }
}

package com.example.fireview.global.config;

import jakarta.servlet.annotation.WebListener;
import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * HTTP 세션 생성/소멸 추적 리스너 (디버그용)
 *
 * OAuth2 state가 어느 세션에 저장되고 언제 세션이 사라지는지 추적.
 * 원인 파악 후 제거 가능.
 */
@Slf4j
@Component
@WebListener
public class SessionEventListener implements HttpSessionListener {

    @Override
    public void sessionCreated(HttpSessionEvent event) {
        log.info("[Session] 생성됨: sessionId={}", event.getSession().getId());
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent event) {
        log.warn("[Session] 소멸됨: sessionId={}", event.getSession().getId());
    }
}

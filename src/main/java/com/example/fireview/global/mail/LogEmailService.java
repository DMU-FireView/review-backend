package com.example.fireview.global.mail;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * 로컬/테스트 환경용 이메일 구현체.
 * app.mail.enabled=false(기본값) 일 때 등록된다.
 * 실제 발송 없이 콘솔에 내용을 출력한다.
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "app.mail.enabled", havingValue = "false", matchIfMissing = true)
public class LogEmailService implements EmailService {

    @Override
    public void sendText(String to, String subject, String text) {
        log.info("[Mail:LOG] to={} | subject={} | body={}", to, subject, text);
    }

    @Override
    public void sendHtml(String to, String subject, String html) {
        log.info("[Mail:LOG] to={} | subject={} | html(len={})", to, subject, html.length());
    }
}

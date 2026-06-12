package com.example.fireview.global.mail;

/**
 * 이메일 발송 추상화 인터페이스.
 * 운영: SmtpEmailService (JavaMailSender)
 * 테스트/로컬: LogEmailService (콘솔 출력)
 */
public interface EmailService {

    /**
     * 단순 텍스트 이메일 발송
     *
     * @param to      수신자 이메일
     * @param subject 제목
     * @param text    본문 (plain text)
     */
    void sendText(String to, String subject, String text);

    /**
     * HTML 이메일 발송
     *
     * @param to      수신자 이메일
     * @param subject 제목
     * @param html    본문 (HTML)
     */
    void sendHtml(String to, String subject, String html);
}

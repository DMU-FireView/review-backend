package com.example.fireview.domain.auth.dto;

/** 앱 전용. 웹은 쿠키로 보내므로 본문이 없다 */
public record RefreshTokenRequest(String refreshToken) {
}

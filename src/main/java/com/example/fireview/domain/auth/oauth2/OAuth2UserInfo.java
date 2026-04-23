package com.example.fireview.domain.auth.oauth2;

public interface OAuth2UserInfo {
    String getProviderId();      // OAuth2 제공자의 사용자 고유 ID
    String getEmail();           // 이메일
    String getName();            // 이름 (닉네임으로 사용)
    String getProfileImageUrl(); // 프로필 이미지 URL
}

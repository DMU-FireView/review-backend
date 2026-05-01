package com.example.fireview.domain.auth.oauth2;

import java.util.Map;

public class NaverOAuth2UserInfo implements OAuth2UserInfo {

    // 네이버는 { "response": { "id": ..., "email": ..., "name": ... } } 구조
    private final Map<String, Object> attributes;

    @SuppressWarnings("unchecked")
    public NaverOAuth2UserInfo(Map<String, Object> attributes) {
        Object response = attributes.get("response");
        if (!(response instanceof Map)) {
            throw new IllegalArgumentException("네이버 OAuth2 응답에 'response' 필드가 없거나 올바르지 않습니다: " + attributes);
        }
        this.attributes = (Map<String, Object>) response;
    }

    @Override
    public String getProviderId() {
        return (String) attributes.get("id");
    }

    @Override
    public String getEmail() {
        return (String) attributes.get("email");
    }

    @Override
    public String getName() {
        return (String) attributes.get("name");
    }

    @Override
    public String getProfileImageUrl() {
        return (String) attributes.get("profile_image");
    }
}

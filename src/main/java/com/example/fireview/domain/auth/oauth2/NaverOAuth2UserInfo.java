package com.example.fireview.domain.auth.oauth2;

import java.util.Map;

public class NaverOAuth2UserInfo implements OAuth2UserInfo {

    // 네이버는 { "response": { "id": ..., "email": ..., "name": ... } } 구조
    private final Map<String, Object> attributes;

    @SuppressWarnings("unchecked")
    public NaverOAuth2UserInfo(Map<String, Object> attributes) {
        this.attributes = (Map<String, Object>) attributes.get("response");
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

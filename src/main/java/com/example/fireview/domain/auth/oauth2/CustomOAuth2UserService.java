package com.example.fireview.domain.auth.oauth2;

import com.example.fireview.domain.user.entity.OAuthProvider;
import com.example.fireview.domain.user.entity.Role;
import com.example.fireview.domain.user.entity.User;
import com.example.fireview.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        // 어떤 제공자인지 확인 (google / naver)
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        OAuthProvider provider;
        try {
            provider = OAuthProvider.valueOf(registrationId.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new OAuth2AuthenticationException("지원하지 않는 OAuth2 제공자입니다: " + registrationId);
        }

        // 제공자별로 사용자 정보 추출
        OAuth2UserInfo userInfo = switch (provider) {
            case GOOGLE -> new GoogleOAuth2UserInfo(oAuth2User.getAttributes());
            case NAVER  -> new NaverOAuth2UserInfo(oAuth2User.getAttributes());
            case LOCAL  -> throw new OAuth2AuthenticationException("LOCAL 계정은 OAuth2 로그인을 사용할 수 없습니다.");
        };

        log.debug("OAuth2 로그인 시도 - provider: {}, email: {}", provider, userInfo.getEmail());

        // DB에서 기존 사용자 조회 → 없으면 신규 가입 처리
        User user = userRepository.findByEmail(userInfo.getEmail())
                .map(existingUser -> updateOAuth2User(existingUser, userInfo))
                .orElseGet(() -> registerOAuth2User(provider, userInfo));

        return new OAuth2UserPrincipal(user, oAuth2User.getAttributes());
    }

    // 기존 사용자 프로필 이미지 업데이트
    private User updateOAuth2User(User user, OAuth2UserInfo userInfo) {
        user.setProfileImageUrl(userInfo.getProfileImageUrl());
        return userRepository.save(user);
    }

    // 신규 OAuth2 사용자 DB 저장
    private User registerOAuth2User(OAuthProvider provider, OAuth2UserInfo userInfo) {
        User newUser = User.builder()
                .email(userInfo.getEmail())
                .nickname(userInfo.getName())
                .profileImageUrl(userInfo.getProfileImageUrl())
                .provider(provider)
                .providerId(userInfo.getProviderId())
                .role(Role.USER)
                .onboardingCompleted(false)
                .build();
        return userRepository.save(newUser);
    }
}

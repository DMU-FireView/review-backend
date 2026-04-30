package com.example.fireview.domain.user.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column
    private String password;  // OAuth2 사용자는 비밀번호 없음

    @Column(nullable = false)
    private String nickname;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OAuthProvider provider;  // LOCAL, GOOGLE, NAVER

    private String providerId;  // OAuth2 제공자의 사용자 고유 ID

    private String profileImageUrl;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private boolean onboardingCompleted;

    /**
     * ATI (Account Trust Index): 앱 사용자의 계정 신뢰도 점수 (0~100)
     * 당근마켓 온도처럼 리뷰어 프로필 옆에 표시됩니다.
     * null = 아직 계산 전
     */
    @Column(name = "ati_score")
    private Double atiScore;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (role == null) role = Role.USER;
    }
}
package com.example.fireview.domain.user.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

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

    @Column(length = 20)
    private String phone;

    /**
     * 챗봇 요금제. null 은 {@link PlanTier#FREE} 과 같게 취급한다.
     *
     * <p>DDL 에 NOT NULL 을 걸지 않는다. 운영 DB 는 ddl-auto=update 로 올라가는데
     * 이미 행이 있는 테이블에 기본값 없는 NOT NULL 컬럼을 추가하면 ALTER 가 실패한다.
     * 제약은 DB 가 아니라 {@link #getEffectivePlan()} 이 책임진다.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "plan_tier", length = 20)
    private PlanTier planTier;

    /** 요금제 만료 시각. null 이면 만료 없음(무료 또는 무기한) */
    @Column(name = "plan_expires_at")
    private LocalDateTime planExpiresAt;

    @Builder.Default
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_interest_categories", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "category")
    private List<String> interestCategories = new java.util.ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (role == null) role = Role.USER;
        if (planTier == null) planTier = PlanTier.FREE;
    }

    /**
     * 지금 실제로 적용되는 요금제.
     *
     * <p>만료 시각이 지난 유료 요금제는 FREE 로 떨어진다. 만료된 행을 배치로
     * 되돌리지 않고 읽을 때 판단하는 쪽을 골랐다. 배치가 밀리거나 죽어도
     * 과금 경계가 틀어지지 않는다.
     */
    public PlanTier getEffectivePlan() {
        if (planTier == null) return PlanTier.FREE;
        if (planTier.isPaid() && planExpiresAt != null
                && planExpiresAt.isBefore(LocalDateTime.now())) {
            return PlanTier.FREE;
        }
        return planTier;
    }

    /** 요금제를 바꾼다. FREE 로 내리면 만료 시각은 의미가 없으므로 비운다. */
    public void changePlan(PlanTier tier, LocalDateTime expiresAt) {
        this.planTier = tier == null ? PlanTier.FREE : tier;
        this.planExpiresAt = this.planTier.isPaid() ? expiresAt : null;
    }
}
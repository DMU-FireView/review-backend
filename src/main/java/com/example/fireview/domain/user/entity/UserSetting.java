package com.example.fireview.domain.user.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * 사용자 설정 엔티티 (User 1:1)
 *
 * 알림 설정, 신뢰 필터 기본값, 화면 경험, 개인정보 동의를 저장합니다.
 */
@Entity
@Table(name = "user_settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    // ── 알림 설정 ─────────────────────────────────────────────────────────────

    /** 위험 상품 알림 — 찜한 상품의 위험 비율이 임계값 초과 시 */
    @Builder.Default
    @Column(nullable = false)
    private boolean notifyRiskyProduct = true;

    /** 분석 완료 알림 — 새 상품 AI 분석 시 */
    @Builder.Default
    @Column(nullable = false)
    private boolean notifyAnalysisComplete = true;

    /** 피드백 처리 알림 — 제출한 신고/피드백 상태 변경 시 */
    @Builder.Default
    @Column(nullable = false)
    private boolean notifyFeedbackResult = true;

    /** 마케팅/추천 알림 — 관심 카테고리 추천 상품 */
    @Builder.Default
    @Column(nullable = false)
    private boolean notifyMarketing = false;

    // ── 신뢰 필터 기본값 ──────────────────────────────────────────────────────

    /** 최소 RTI 기준 (0~100) — 이 기준 미만 리뷰 숨김 */
    @Builder.Default
    @Column(nullable = false)
    private int rtiThreshold = 50;

    /** 위험 리뷰 기본 숨김 — RTI 50 미만 리뷰를 접힘 상태로 표시 */
    @Builder.Default
    @Column(nullable = false)
    private boolean hideRiskyReviews = true;

    /** 의심 리뷰 라벨 표시 — 구매 비확인 행동이 여럿인 경우 작은 라벨 */
    @Builder.Default
    @Column(nullable = false)
    private boolean showSuspiciousLabel = true;

    /** 구매확인 리뷰 우선 표시 */
    @Builder.Default
    @Column(nullable = false)
    private boolean prioritizeVerifiedReviews = true;

    /** 분석 팝업 자동 열기 — 위험 리뷰 클릭 시 분석 상세 자동 표시 */
    @Builder.Default
    @Column(nullable = false)
    private boolean autoOpenAnalysisPopup = false;

    // ── 화면 및 사용 경험 ─────────────────────────────────────────────────────

    /** 상품 카드 밀도 (COMFORTABLE / COMPACT) */
    @Builder.Default
    @Column(nullable = false, length = 20)
    private String cardDensity = "COMFORTABLE";

    /** 리뷰 기본 정렬 (VERIFIED_RECENT / RECENT / HELPFUL) */
    @Builder.Default
    @Column(nullable = false, length = 30)
    private String reviewSortOrder = "VERIFIED_RECENT";

    /** RTI 라벨 표시 방식 (BADGE_SMALL / BADGE_LARGE / NONE) */
    @Builder.Default
    @Column(nullable = false, length = 20)
    private String rtiLabelStyle = "BADGE_SMALL";

    /** 테마 (SYSTEM / LIGHT / DARK) */
    @Builder.Default
    @Column(nullable = false, length = 10)
    private String theme = "SYSTEM";

    // ── 개인정보 동의 ─────────────────────────────────────────────────────────

    /** 리뷰 분석 데이터 활용 동의 */
    @Builder.Default
    @Column(nullable = false)
    private boolean allowDataAnalysis = true;
}

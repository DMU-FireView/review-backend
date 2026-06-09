package com.example.fireview.domain.admin.dto.response;

import java.util.List;

/**
 * 관리자 모델 성능 모니터링 응답 DTO
 *
 * <p>AI 분석 모델의 성능을 DB 집계 기반으로 제공합니다.
 * <ul>
 *   <li>RTI 분포 — SAFE / SUSPICIOUS / DANGER 비율</li>
 *   <li>사용자 동의율 — AI 판정과 사용자 피드백의 일치 비율</li>
 *   <li>분석 피드백 현황 — 이의 제기 처리 통계</li>
 *   <li>신뢰도 추이 — 최근 7일 평균 RTI 점수</li>
 * </ul>
 */
public record AdminModelPerformanceResponse(

        // ── 전체 요약 ───────────────────────────────────────────────
        long totalAnalyzedReviews,
        double averageRtiScore,

        // ── RTI 등급 분포 ────────────────────────────────────────────
        RtiDistribution rtiDistribution,

        // ── 사용자 동의율 (AI 판정 vs 사용자 REAL/FAKE 피드백) ────────
        UserAgreementStats userAgreement,

        // ── 분석 피드백 처리 현황 ────────────────────────────────────
        AnalysisFeedbackStats analysisFeedbackStats,

        // ── 최근 7일 일별 평균 RTI 추이 ──────────────────────────────
        List<DailyRtiTrend> dailyRtiTrend

) {

    public record RtiDistribution(
            long safeCount,
            long suspiciousCount,
            long dangerCount,
            double safePercent,
            double suspiciousPercent,
            double dangerPercent
    ) {}

    public record UserAgreementStats(
            long totalFeedbacks,
            long agreementCount,   // AI SAFE → 사용자 REAL, AI DANGER/SUSPICIOUS → 사용자 FAKE
            long disagreementCount,
            double agreementRate   // 0.0 ~ 100.0
    ) {}

    public record AnalysisFeedbackStats(
            long submitted,
            long underReview,
            long resolved,
            long rejected,
            double resolutionRate  // (resolved + rejected) / total * 100
    ) {}

    public record DailyRtiTrend(
            String date,           // yyyy-MM-dd
            double averageRti,
            long reviewCount
    ) {}
}

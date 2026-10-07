package com.example.fireview.domain.review.entity;

public enum TrustGrade {
    SAFE("안전", "#22C55E"),
    SUSPICIOUS("의심", "#EAB308"),
    DANGER("위험", "#EF4444");

    private final String label;
    private final String color;

    TrustGrade(String label, String color) {
        this.label = label;
        this.color = color;
    }

    public String getLabel() { return label; }
    public String getColor() { return color; }

    /**
     * 등급 경계. AI 서버 등급 정책(rti-v0)·Data 서버와 같은 값을 쓴다.
     * 다르면 같은 RTI 가 상세에선 safe, 목록에선 SUSPICIOUS 처럼 서로 다르게 그려진다.
     *
     * <p>이미 저장된 {@code reviews.trust_grade} 는 코드가 다시 매기지 않는다(#201).
     * ReviewResponse·AdminService 는 저장값을 그대로 쓴다. 대부분은 더미 시드(DataInitializer,
     * #195 에서 삭제 예정)이거나 레거시 AiAnalysisService 가 AI level 을 85/55/30 으로 바꿔 저장한
     * 것이라 80/50 과 70/40 에서 등급이 같다. 레거시 updateReviewRtiScores 경로로 실제 rti 가
     * 경계 구간(40~50, 70~80)에 저장된 행은 docs/sql/reclassify-review-trust-grade.sql 로
     * 확인·재분류한다.
     */
    public static final double SAFE_MIN_SCORE = 70;
    public static final double SUSPICIOUS_MIN_SCORE = 40;

    public static TrustGrade fromScore(double score) {
        if (score >= SAFE_MIN_SCORE) return SAFE;
        if (score >= SUSPICIOUS_MIN_SCORE) return SUSPICIOUS;
        return DANGER;
    }

    /**
     * AI 서버 level 문자열 (safe/warn/danger) → TrustGrade 변환.
     *
     * @return level 이 null 이면 null. AI 가 세 신호 모두 계산하지 못한 "판단 불가"라서
     *         어느 등급으로도 바꾸지 않는다. 호출하는 쪽이 null 을 처리해야 한다
     */
    public static TrustGrade fromAiLevel(String level) {
        if (level == null) return null;
        return switch (level.toLowerCase()) {
            case "safe"   -> SAFE;
            case "danger" -> DANGER;
            default       -> SUSPICIOUS; // warn
        };
    }

    /** AI 서버 RTI 정수값 → TrustGrade 변환 */
    public static TrustGrade fromRti(int rti) {
        return fromScore(rti);
    }

    /** AI 서버 형식 level 문자열로 변환 (safe | warn | danger) */
    public String toLevel() {
        return switch (this) {
            case SAFE -> "safe";
            case DANGER -> "danger";
            default -> "warn";
        };
    }
}
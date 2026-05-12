package com.example.fireview.domain.ai.dto.response;

/**
 * 주요 판단 신호 DTO (프론트엔드 TrustSignal 엔티티 매핑)
 *
 * product-detail 분석 결과를 집계하여 생성:
 * - 구매인증 리뷰 비율  : input_features.verified_purchase == "True" 비율
 * - 텍스트 다양성      : REPETITIVE_KEYWORD 없는 리뷰 비율
 * - 반복 표현 비율     : REPETITIVE_KEYWORD 포함 리뷰 비율
 * - 작성 시점 패턴     : MULTIPLE_REVIEWS_SAME_DAY 포함 리뷰 비율
 */
public record TrustSignalDto(
        String label,       // 신호 이름
        String value,       // 판정 값 (높음 / 보통 / 낮음 / 자연스러움 / 부자연스러움)
        boolean isPositive  // 긍정 여부 → 프론트 색상/아이콘 결정
) {

    /** 구매인증 리뷰 비율 */
    public static TrustSignalDto ofVerifiedPurchase(double ratio) {
        if (ratio >= 0.6) return new TrustSignalDto("구매인증 리뷰 비율", "높음", true);
        if (ratio >= 0.3) return new TrustSignalDto("구매인증 리뷰 비율", "보통", false);
        return new TrustSignalDto("구매인증 리뷰 비율", "낮음", false);
    }

    /** 텍스트 다양성 (반복 표현 없는 비율) */
    public static TrustSignalDto ofTextDiversity(double ratio) {
        if (ratio >= 0.7) return new TrustSignalDto("텍스트 다양성", "높음", true);
        if (ratio >= 0.4) return new TrustSignalDto("텍스트 다양성", "보통", false);
        return new TrustSignalDto("텍스트 다양성", "낮음", false);
    }

    /** 반복 표현 비율 */
    public static TrustSignalDto ofRepetitivePattern(double ratio) {
        if (ratio < 0.3) return new TrustSignalDto("반복 표현 비율", "낮음", true);
        if (ratio < 0.6) return new TrustSignalDto("반복 표현 비율", "보통", false);
        return new TrustSignalDto("반복 표현 비율", "높음", false);
    }

    /** 작성 시점 패턴 (동일 날짜 다수 작성 비율) */
    public static TrustSignalDto ofTimingPattern(double ratio) {
        if (ratio < 0.3) return new TrustSignalDto("작성 시점 패턴", "자연스러움", true);
        if (ratio < 0.6) return new TrustSignalDto("작성 시점 패턴", "다소 부자연스러움", false);
        return new TrustSignalDto("작성 시점 패턴", "부자연스러움", false);
    }
}

package com.example.fireview.domain.ai.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/**
 * 레거시: 새 구조에서는 Data 서버가 ai.re-view.kr 를 호출. 이 경로는 프론트 옛 상세 화면 호환용.
 * AI 서버 개별 리뷰 분석 결과 (product-detail 응답 내 items)
 */
public record AiAnalysisResult(

        @JsonProperty("review_id")
        String reviewId,

        @JsonProperty("product_id")
        String productId,

        @JsonProperty("user_id")
        String userId,

        Integer rating,

        @JsonProperty("review_date")
        String reviewDate,

        Integer rti,        // 신뢰도 점수 (0~100)
        String level,       // safe | warn | danger

        AiSignals signals,
        List<AiReason> reasons,

        @JsonProperty("input_features")
        Map<String, Object> inputFeatures

) {}

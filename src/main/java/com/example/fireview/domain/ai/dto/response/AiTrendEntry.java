package com.example.fireview.domain.ai.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 레거시: 새 구조에서는 Data 서버가 ai.re-view.kr 를 호출. 이 경로는 프론트 옛 상세 화면 호환용.
 * AI 서버 날짜별 추이 데이터 (rti-trend 응답 내 items)
 */
public record AiTrendEntry(

        String date,            // YYYY-MM-DD

        @JsonProperty("average_rti")
        Double averageRti,

        @JsonProperty("review_count")
        Integer reviewCount,

        @JsonProperty("safe_count")
        Integer safeCount,

        @JsonProperty("warn_count")
        Integer warnCount,

        @JsonProperty("danger_count")
        Integer dangerCount

) {}

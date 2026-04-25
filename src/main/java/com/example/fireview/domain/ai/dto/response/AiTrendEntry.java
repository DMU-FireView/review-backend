package com.example.fireview.domain.ai.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
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

package com.example.fireview.domain.ai.dto.response;

/**
 * 핵심 요약 통계 스냅샷 지표 (명세서 v11.0 §3.5 SummaryStat)
 */
public record SummaryStat(
        Integer total_reviews,
        Double average_rti,
        Integer danger_count,
        Integer warn_count,
        Integer safe_count
) {}

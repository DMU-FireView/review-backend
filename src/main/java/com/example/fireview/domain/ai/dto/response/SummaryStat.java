package com.example.fireview.domain.ai.dto.response;

/**
 * 레거시: 새 구조에서는 Data 서버가 ai.re-view.kr 를 호출. 이 경로는 프론트 옛 상세 화면 호환용.
 * 핵심 요약 통계 스냅샷 지표 (명세서 v11.0 §3.5 SummaryStat)
 */
public record SummaryStat(
        Integer total_reviews,
        Double average_rti,
        Integer danger_count,
        Integer warn_count,
        Integer safe_count
) {}

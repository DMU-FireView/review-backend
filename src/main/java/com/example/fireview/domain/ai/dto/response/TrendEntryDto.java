package com.example.fireview.domain.ai.dto.response;

/**
 * 프론트엔드 전달용 RTI 추이 DTO
 * AI 서버 응답(AiTrendEntry)에서 snake_case를 camelCase로 변환하여 전달합니다.
 */
public record TrendEntryDto(

        String date,
        Double averageRti,
        Integer reviewCount,
        Integer safeCount,
        Integer warnCount,
        Integer dangerCount

) {
    public static TrendEntryDto from(AiTrendEntry entry) {
        return new TrendEntryDto(
                entry.date() != null ? entry.date() : "",
                entry.averageRti() != null ? entry.averageRti() : 50.0,
                entry.reviewCount() != null ? entry.reviewCount() : 0,
                entry.safeCount() != null ? entry.safeCount() : 0,
                entry.warnCount() != null ? entry.warnCount() : 0,
                entry.dangerCount() != null ? entry.dangerCount() : 0
        );
    }
}

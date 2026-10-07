package com.example.fireview.domain.ai.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * 레거시: 새 구조에서는 Data 서버가 ai.re-view.kr 를 호출. 이 경로는 프론트 옛 상세 화면 호환용.
 * 리뷰 상세 분석 리포트 응답 (명세서 v11.0 §3.4)
 *
 * AI 서버 엔드포인트: POST /api/internal/ai/reviews/report
 *
 * MVP 스펙에서 highlights 및 percentage 필드가 제외됨.
 * - reasons: 상세 감점 사유 설명 카드 리스트 (ReasonDetail)
 */
public record AiReviewReportResponse(

        @JsonProperty("review_id")
        String reviewId,

        Integer rti,

        AiSignals signals,

        List<ReasonDetail> reasons

) {}

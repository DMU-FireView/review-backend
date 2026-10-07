package com.example.fireview.domain.ai.dto.response;

import java.util.List;

/**
 * 레거시: 새 구조에서는 Data 서버가 ai.re-view.kr 를 호출. 이 경로는 프론트 옛 상세 화면 호환용.
 * AI 서버 product-detail API 응답
 * POST /api/internal/ai/reviews/product-detail
 */
public record AiProductDetailResponse(
        List<AiAnalysisResult> results
) {}

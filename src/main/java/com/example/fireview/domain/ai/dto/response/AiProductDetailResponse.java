package com.example.fireview.domain.ai.dto.response;

import java.util.List;

/**
 * AI 서버 product-detail API 응답
 * POST /api/internal/ai/reviews/product-detail
 */
public record AiProductDetailResponse(
        List<AiAnalysisResult> results
) {}

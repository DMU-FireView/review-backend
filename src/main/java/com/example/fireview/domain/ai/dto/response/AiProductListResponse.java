package com.example.fireview.domain.ai.dto.response;

import java.util.List;

/**
 * AI 서버 product-list API 응답
 * POST /api/internal/ai/products/product-list
 */
public record AiProductListResponse(
        List<AiProductSummary> products
) {}

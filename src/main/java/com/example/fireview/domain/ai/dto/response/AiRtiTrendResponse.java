package com.example.fireview.domain.ai.dto.response;

import java.util.List;

/**
 * AI 서버 rti-trend API 응답
 * POST /api/internal/ai/products/rti-trend
 */
public record AiRtiTrendResponse(
        List<AiTrendEntry> trend
) {}

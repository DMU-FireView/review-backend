package com.example.fireview.domain.ai.dto.request;

import java.util.List;

/**
 * AI 서버에 전달하는 분석 요청 DTO
 * reviews 배열에 리뷰 데이터를 담아 전송합니다.
 */
public record AiAnalyzeRequest(
        List<AiReviewItem> reviews
) {}

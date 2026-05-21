package com.example.fireview.domain.ai.dto.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * AI 서버에 전달하는 분석 요청 DTO
 * reviews 배열과 page_url(상품 URL)을 함께 전송합니다.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AiAnalyzeRequest(

        @JsonProperty("page_url")
        String pageUrl,         // 상품 페이지 URL (optional, AI 서버 크롤링 보조)

        List<AiReviewItem> reviews

) {}

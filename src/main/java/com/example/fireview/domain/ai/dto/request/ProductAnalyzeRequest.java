package com.example.fireview.domain.ai.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * 프론트엔드 → 백엔드 분석 요청 DTO
 * 네이버 상품 ID를 받아 AI 서버에 크롤링+분석을 요청합니다.
 *
 * @param productId  분석할 상품 ID (필수)
 * @param productUrl 상품 페이지 URL (선택 - AI 서버 크롤링 보조용)
 */
public record ProductAnalyzeRequest(

        @NotBlank(message = "productId는 필수입니다.")
        String productId,

        String productUrl   // optional

) {}

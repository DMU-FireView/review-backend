package com.example.fireview.domain.ai.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * 프론트엔드 → 백엔드 분석 요청 DTO
 * 네이버 상품 ID를 받아 AI 서버에 크롤링+분석을 요청합니다.
 */
public record ProductAnalyzeRequest(

        @NotBlank(message = "productId는 필수입니다.")
        String productId

) {}

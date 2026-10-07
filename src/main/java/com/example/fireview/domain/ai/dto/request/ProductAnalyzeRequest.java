package com.example.fireview.domain.ai.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * 레거시: 새 구조에서는 Data 서버가 ai.re-view.kr 를 호출. 이 경로는 프론트 옛 상세 화면 호환용.
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

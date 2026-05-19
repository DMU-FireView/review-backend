package com.example.fireview.domain.ai.controller;

import com.example.fireview.domain.ai.dto.request.ProductAnalyzeRequest;
import com.example.fireview.domain.ai.dto.response.ProductAnalysisResponse;
import com.example.fireview.domain.ai.service.AiAnalysisService;
import com.example.fireview.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * AI 분석 컨트롤러
 *
 * 프론트엔드가 productId를 보내면,
 * BE가 AI 서버에 크롤링+분석을 요청하고 결과를 반환합니다.
 */
@Slf4j
@RestController
@RequestMapping("/api/analysis")
@RequiredArgsConstructor
public class AiAnalysisController {

    private final AiAnalysisService aiAnalysisService;

    /**
     * 상품 분석 요청
     *
     * [흐름]
     * 프론트 → POST /api/analysis/product { "productId": "7195971829" }
     *        → BE가 AI 서버에 크롤링+분석 요청
     *        → AI 서버 결과를 프론트에 반환
     *
     * @param request productId (네이버 상품 ID 또는 내부 DB ID)
     * @return 분석 결과 (평균 RTI, 등급, 리뷰 목록, 추이 데이터)
     */
    @PostMapping("/product")
    public ResponseEntity<ApiResponse<ProductAnalysisResponse>> analyzeProduct(
            @Valid @RequestBody ProductAnalyzeRequest request
    ) {
        log.info("[AiAnalysisController] 분석 요청: productId={}", request.productId());

        ProductAnalysisResponse response = aiAnalysisService.analyzeProduct(request.productId());

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}

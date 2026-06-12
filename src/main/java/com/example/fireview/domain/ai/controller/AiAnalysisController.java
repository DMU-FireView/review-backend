package com.example.fireview.domain.ai.controller;

import com.example.fireview.domain.ai.dto.request.ProductAnalyzeRequest;
import com.example.fireview.domain.ai.dto.response.ProductAnalysisResponse;
import com.example.fireview.domain.ai.service.AiAnalysisService;
import com.example.fireview.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
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
     * 프론트 → POST /api/analysis/product { "productId": "7195971829", "productUrl": "https://..." }
     *        → BE가 AI 서버에 크롤링+분석 요청 (page_url 포함)
     *        → AI 서버 결과를 프론트에 반환
     *        → 로그인 사용자의 경우 분석 완료 알림 발송 (notifyAnalysisComplete 설정 확인)
     *
     * @param request productId (필수), productUrl (선택 - AI 서버 크롤링 보조)
     * @param jwt     로그인 사용자 토큰 (비로그인 시 null — permitAll 엔드포인트)
     * @return 분석 결과 (평균 RTI, 등급, 리뷰 목록, 추이 데이터)
     */
    @PostMapping("/product")
    public ResponseEntity<ApiResponse<ProductAnalysisResponse>> analyzeProduct(
            @Valid @RequestBody ProductAnalyzeRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        String userEmail = (jwt != null) ? jwt.getSubject() : null;
        log.info("[AiAnalysisController] 분석 요청: productId={}, userEmail={}", request.productId(), userEmail);

        ProductAnalysisResponse response = aiAnalysisService.analyzeProduct(
                request.productId(), request.productUrl(), userEmail);

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}

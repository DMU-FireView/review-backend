package com.example.fireview.domain.search.service;

import com.example.fireview.domain.dashboard.service.DashboardService;
import com.example.fireview.domain.product.client.NaverShoppingClient;
import com.example.fireview.domain.product.dto.ProductResponse;
import com.example.fireview.domain.search.dto.NaverSearchResponse;
import com.example.fireview.global.exception.CustomException;
import com.example.fireview.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NaverSearchService {

    private final NaverShoppingClient naverShoppingClient;
    private final DashboardService dashboardService;

    /**
     * 네이버 쇼핑 API로 상품 검색.
     *
     * @param keyword 검색어 (필수, 공백 불가)
     * @param display 결과 수 (기본 30, 최대 100)
     * @return 네이버 쇼핑 검색 결과
     */
    public NaverSearchResponse search(String keyword, int display) {
        if (keyword == null || keyword.isBlank()) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }

        if (!naverShoppingClient.isConfigured()) {
            log.warn("[NaverSearch] API 키 미설정 - 검색 불가: {}", keyword);
            throw new CustomException(ErrorCode.NAVER_API_NOT_CONFIGURED);
        }

        log.info("[NaverSearch] 검색 시작: keyword={}, display={}", keyword, display);

        // 검색 키워드 인기도 기록 (비동기 처리 가능하나 현재는 동기)
        try {
            dashboardService.recordKeywordSearch(keyword);
        } catch (Exception e) {
            log.warn("[NaverSearch] 키워드 기록 실패 (무시): {}", e.getMessage());
        }

        List<ProductResponse> products = naverShoppingClient
                .searchProducts(keyword, display)
                .stream()
                .map(ProductResponse::fromNaverItem)
                .toList();

        log.info("[NaverSearch] 검색 완료: keyword={}, 결과={}건", keyword, products.size());

        return NaverSearchResponse.of(keyword, products);
    }
}

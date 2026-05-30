package com.example.fireview.domain.product.service;

import com.example.fireview.domain.product.client.NaverShoppingClient;
import com.example.fireview.domain.product.dto.ProductResponse;
import com.example.fireview.domain.product.entity.Product;
import com.example.fireview.domain.product.repository.ProductRepository;
import com.example.fireview.global.exception.CustomException;
import com.example.fireview.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;
    private final NaverShoppingClient naverShoppingClient;

    public Product findById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));
    }

    public ProductResponse getProduct(Long id) {
        return ProductResponse.from(findById(id));
    }

    @Cacheable(value = "productList", key = "'all'")
    public List<ProductResponse> getAllProducts() {
        return productRepository.findAll().stream()
                .map(ProductResponse::from)
                .toList();
    }

    /**
     * 상품 검색.
     *
     * 1순위: 네이버 쇼핑 검색 API → 실시간 상품 목록 반환 (API 키 설정 시)
     * 2순위: 로컬 DB 검색 → API 키 미설정 또는 API 오류 시 fallback
     */
    public List<ProductResponse> searchProducts(String keyword) {
        if (naverShoppingClient.isConfigured()) {
            log.debug("[Search] 네이버 쇼핑 API 검색: {}", keyword);
            List<ProductResponse> naverResults = naverShoppingClient
                    .searchProducts(keyword, 30)
                    .stream()
                    .map(ProductResponse::fromNaverItem)
                    .toList();

            if (!naverResults.isEmpty()) {
                return naverResults;
            }
            log.warn("[Search] 네이버 API 결과 없음, DB 검색으로 fallback: {}", keyword);
        }

        log.debug("[Search] 로컬 DB 검색: {}", keyword);
        return productRepository.findByNameContainingIgnoreCase(keyword).stream()
                .map(ProductResponse::from)
                .toList();
    }

    @Transactional
    public void updateAvgRti(Long productId, double newAvgRti) {
        Product product = findById(productId);
        product.setAvgRti(newAvgRti);
        productRepository.save(product);
    }
}
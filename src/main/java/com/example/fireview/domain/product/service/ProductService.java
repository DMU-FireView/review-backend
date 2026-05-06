package com.example.fireview.domain.product.service;

import com.example.fireview.domain.product.dto.ProductResponse;
import com.example.fireview.domain.product.entity.Category;
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

    /** 로컬 DB 상품명 검색 (platformLinks JOIN FETCH로 LazyInit 방지) */
    public List<ProductResponse> searchProducts(String keyword) {
        return productRepository.findByNameContainingIgnoreCaseWithLinks(keyword).stream()
                .map(ProductResponse::from)
                .toList();
    }

    @Transactional
    public void updateAvgRti(Long productId, double newAvgRti) {
        Product product = findById(productId);
        product.setAvgRti(newAvgRti);
        productRepository.save(product);
    }

    /**
     * 네이버 캐시 상품을 DB에 저장하고 저장된 ProductResponse 반환.
     * 이미 naverProductId로 저장된 상품이 있으면 기존 상품 반환 (중복 저장 방지).
     */
    @Transactional
    public ProductResponse saveFromCache(ProductResponse cached) {
        // 중복 저장 방지: naverProductId로 먼저 조회
        if (cached.naverProductId() != null) {
            return productRepository.findByNaverProductId(cached.naverProductId())
                    .map(existing -> {
                        log.info("[ProductService] 이미 DB에 존재하는 네이버 상품: naverProductId={}", cached.naverProductId());
                        return ProductResponse.from(existing);
                    })
                    .orElseGet(() -> doSaveFromCache(cached));
        }
        return doSaveFromCache(cached);
    }

    private ProductResponse doSaveFromCache(ProductResponse cached) {
        Category category = cached.category() != null ? cached.category() : Category.ETC;
        Product product = Product.builder()
                .name(cached.name())
                .imageUrl(cached.imageUrl())
                .price(cached.price())
                .category(category)
                .platform(cached.platform() != null ? cached.platform() : "NAVER")
                .naverProductId(cached.naverProductId())
                .subCategory(cached.subCategory())
                .avgRti(50.0)
                .reviewCount(0)
                .avgRating(0.0)
                .build();
        Product saved = productRepository.save(product);
        log.info("[ProductService] 네이버 캐시 상품 DB 저장 완료: id={}, naverProductId={}", saved.getId(), saved.getNaverProductId());
        return ProductResponse.from(saved);
    }
}
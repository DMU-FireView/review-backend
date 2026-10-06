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
        // DB PK로 먼저 조회, 없으면 naverProductId로 fallback 조회
        return productRepository.findById(id)
                .orElseGet(() -> productRepository.findByNaverProductId(String.valueOf(id))
                        .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND)));
    }

    public ProductResponse getProduct(Long id) {
        return ProductResponse.from(findById(id));
    }

    /**
     * 홈 목록.
     *
     * <p>Data 서버 상품이 하나라도 있으면 그것만 보여준다. 더미와 섞으면 사용자가 진짜와
     * 가짜를 구분할 수 없다. 아직 하나도 없을 때만 기존 더미를 보여준다 — 빈 홈보다는 낫다.
     *
     * <p>Data 서버에는 "수집된 상품 전체 목록" API 가 없어서, 검색으로 들어와 번호표가
     * 붙은 상품이 홈의 원천이다. 검색이 쌓일수록 홈이 채워진다. 최근 100건만 보인다.
     *
     * <p>캐시는 검색·상세 조회로 표시 정보가 바뀔 때 {@code ProductTagRegistry} 가 비운다.
     */
    @Cacheable(value = "productList", key = "'all'")
    public List<ProductResponse> getAllProducts() {
        List<Product> fromDataServer = productRepository.findTop100ByDataPlatformIsNotNullOrderByCreatedAtDesc();
        List<Product> source = fromDataServer.isEmpty() ? productRepository.findAll() : fromDataServer;
        return source.stream()
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
        // naverProductId를 DB PK로 직접 사용
        Long productId = cached.id() != null ? cached.id() : Long.parseLong(cached.naverProductId());
        Product product = Product.builder()
                .id(productId)
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
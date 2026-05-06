package com.example.fireview.domain.product.cache;

import com.example.fireview.domain.product.dto.ProductResponse;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 네이버 검색 결과 상품 인메모리 캐시
 *
 * 네이버 API로 가져온 상품은 DB에 저장되지 않으므로,
 * 상품 상세 조회(/api/products/{id}) 시 DB에 없으면 이 캐시를 fallback으로 사용한다.
 */
@Component
public class NaverProductCache {

    private final Map<Long, ProductResponse> cache = new ConcurrentHashMap<>();

    public void putAll(List<ProductResponse> products) {
        products.forEach(p -> {
            if (p.id() != null && p.id() > 0) {
                cache.put(p.id(), p);
            }
        });
    }

    public Optional<ProductResponse> get(Long id) {
        return Optional.ofNullable(cache.get(id));
    }
}

package com.example.fireview.domain.product.cache;

import com.example.fireview.domain.product.dto.ProductResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * 네이버 검색 결과 상품 Redis 캐시
 *
 * 네이버 API로 가져온 상품은 DB에 저장되지 않으므로,
 * 상품 상세 조회(/api/products/{id}) 시 DB에 없으면 이 캐시를 fallback으로 사용한다.
 *
 * - 키: "naver:product:{id}"
 * - 값: ProductResponse JSON 문자열
 * - TTL: 24시간
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NaverProductCache {

    private static final String KEY_PREFIX = "naver:product:";
    private static final Duration TTL = Duration.ofHours(24);

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    public void putAll(List<ProductResponse> products) {
        products.forEach(p -> {
            if (p.id() != null && p.id() > 0) {
                String key = KEY_PREFIX + p.id();
                try {
                    String json = objectMapper.writeValueAsString(p);
                    redisTemplate.opsForValue().set(key, json, TTL);
                } catch (Exception e) {
                    log.warn("[NaverProductCache] Redis 저장 실패 (무시): id={}, {}", p.id(), e.getMessage());
                }
            }
        });
    }

    public Optional<ProductResponse> get(Long id) {
        String key = KEY_PREFIX + id;
        try {
            String json = redisTemplate.opsForValue().get(key);
            if (json != null) {
                return Optional.of(objectMapper.readValue(json, ProductResponse.class));
            }
        } catch (Exception e) {
            log.warn("[NaverProductCache] Redis 조회 실패 (무시): id={}, {}", id, e.getMessage());
        }
        return Optional.empty();
    }
}

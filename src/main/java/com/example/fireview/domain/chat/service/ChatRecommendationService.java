package com.example.fireview.domain.chat.service;

import com.example.fireview.domain.dataserver.DataServerProductKey;
import com.example.fireview.domain.product.entity.Product;
import com.example.fireview.domain.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * 챗봇이 "비슷한 상품"을 보여줄 때 실제 상품을 고른다.
 *
 * <p>모델은 추천이 필요한지만 판단하고, 상품은 여기서 DB 로 고른다. 모델에게 상품을
 * 고르게 하면 없는 상품 ID·링크를 지어낼 수 있기 때문이다. LLM 을 더 부르지 않는다.
 *
 * <p>기준: 대화 상품과 같은 카테고리, 가격이 0.5~1.5배, Data 서버 번호표가 있는 상품,
 * 대화 상품 제외, 리뷰 많은 순 최대 3개. RTI 가 아직 없어 신뢰도로는 정렬하지 않는다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatRecommendationService {

    public static final int MAX_RECOMMENDATIONS = 3;

    static final double MIN_PRICE_RATIO = 0.5;
    static final double MAX_PRICE_RATIO = 1.5;

    private final ProductRepository productRepository;

    /**
     * @param externalId 대화 상품 식별자 {@code "{platform}-{productId}"}
     * @return 대화 상품을 못 찾거나 카테고리가 없으면 빈 목록
     */
    public List<ChatRecommendation> findSimilar(String externalId) {
        Optional<Product> current = DataServerProductKey.parse(externalId)
                .flatMap(key -> productRepository.findByDataPlatformAndDataProductId(
                        key.platform(), key.productId()));
        if (current.isEmpty() || current.get().getCategory() == null) {
            log.debug("[Chat] 추천 기준 상품이 없거나 카테고리가 없다 - productId={}", externalId);
            return List.of();
        }
        return findCandidates(current.get()).stream()
                .map(ChatRecommendation::from)
                .toList();
    }

    private List<Product> findCandidates(Product current) {
        Limit limit = Limit.of(MAX_RECOMMENDATIONS);
        Long price = current.getPrice();
        if (price == null || price <= 0) {
            return productRepository.findRecommendationCandidates(
                    current.getCategory(), current.getId(), limit);
        }
        return productRepository.findRecommendationCandidatesInPriceRange(
                current.getCategory(), current.getId(),
                (long) Math.ceil(price * MIN_PRICE_RATIO),
                (long) Math.floor(price * MAX_PRICE_RATIO),
                limit);
    }
}

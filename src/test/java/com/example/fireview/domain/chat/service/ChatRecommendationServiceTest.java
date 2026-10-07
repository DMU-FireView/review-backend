package com.example.fireview.domain.chat.service;

import com.example.fireview.domain.product.entity.Category;
import com.example.fireview.domain.product.entity.Product;
import com.example.fireview.domain.product.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 추천 선정 규칙은 대부분 쿼리에 있으므로 실제 JPA 쿼리로 검증한다(H2).
 */
@DataJpaTest
@ActiveProfiles("test")
class ChatRecommendationServiceTest {

    private static final String CURRENT = "kurly-1000";

    @Autowired ProductRepository productRepository;

    private ChatRecommendationService service;
    private long nextId = 1;

    @BeforeEach
    void setUp() {
        service = new ChatRecommendationService(productRepository);
    }

    private Product save(String platform, String productId, Category category, Long price, Integer reviewCount) {
        Product product = Product.builder()
                .id(nextId++).name("상품 " + productId).category(category)
                .platform("KURLY").price(price).reviewCount(reviewCount)
                .imageUrl("https://img.example/" + productId + ".jpg")
                .dataPlatform(platform).dataProductId(productId)
                .build();
        return productRepository.saveAndFlush(product);
    }

    private Product current(Long price) {
        return save("kurly", "1000", Category.BEAUTY_SKINCARE, price, 50);
    }

    private static List<String> ids(List<ChatRecommendation> recommendations) {
        return recommendations.stream().map(ChatRecommendation::externalId).toList();
    }

    @Test
    void 같은_카테고리_상품만_고른다() {
        current(20000L);
        save("kurly", "2001", Category.BEAUTY_SKINCARE, 20000L, 10);
        save("kurly", "2002", Category.BEAUTY_MAKEUP, 20000L, 999);

        assertThat(ids(service.findSimilar(CURRENT))).containsExactly("kurly-2001");
    }

    @Test
    void 가격이_절반에서_한배반_사이인_상품만_고른다() {
        current(20000L);
        save("kurly", "low-in", Category.BEAUTY_SKINCARE, 10000L, 1);
        save("kurly", "high-in", Category.BEAUTY_SKINCARE, 30000L, 2);
        save("kurly", "low-out", Category.BEAUTY_SKINCARE, 9999L, 100);
        save("kurly", "high-out", Category.BEAUTY_SKINCARE, 30001L, 100);
        save("kurly", "no-price", Category.BEAUTY_SKINCARE, null, 100);

        assertThat(ids(service.findSimilar(CURRENT)))
                .containsExactlyInAnyOrder("kurly-low-in", "kurly-high-in");
    }

    @Test
    void 현재_가격을_모르면_가격_조건을_빼고_고른다() {
        current(null);
        save("kurly", "cheap", Category.BEAUTY_SKINCARE, 100L, 3);
        save("kurly", "pricey", Category.BEAUTY_SKINCARE, 999999L, 2);
        save("kurly", "no-price", Category.BEAUTY_SKINCARE, null, 1);

        assertThat(ids(service.findSimilar(CURRENT)))
                .containsExactly("kurly-cheap", "kurly-pricey", "kurly-no-price");
    }

    @Test
    void 현재_상품은_빼고_고른다() {
        current(20000L);
        save("kurly", "2001", Category.BEAUTY_SKINCARE, 20000L, 1);

        assertThat(ids(service.findSimilar(CURRENT)))
                .doesNotContain(CURRENT)
                .containsExactly("kurly-2001");
    }

    @Test
    void 번호표가_없는_상품은_고르지_않는다() {
        // 상세 화면(/product/:platform/:productId)을 열 수 없는 상품이다
        current(20000L);
        save(null, null, Category.BEAUTY_SKINCARE, 20000L, 500);
        save("", "", Category.BEAUTY_SKINCARE, 20000L, 400);
        save("kurly", "2001", Category.BEAUTY_SKINCARE, 20000L, 1);

        assertThat(ids(service.findSimilar(CURRENT))).containsExactly("kurly-2001");
    }

    @Test
    void 공백뿐인_번호표는_LIMIT_전에_빠지고_유효한_후보가_세_자리를_채운다() {
        // 리뷰가 가장 많은 행들이 공백 번호표면 쿼리 단계에서 빠져야 뒤의 유효 후보가 자리를 채운다
        current(20000L);
        save(" ", " ", Category.BEAUTY_SKINCARE, 20000L, 900);
        save("kurly", "  ", Category.BEAUTY_SKINCARE, 20000L, 800);
        save("   ", "2999", Category.BEAUTY_SKINCARE, 20000L, 700);
        save("kurly", "2001", Category.BEAUTY_SKINCARE, 20000L, 30);
        save("kurly", "2002", Category.BEAUTY_SKINCARE, 20000L, 20);
        save("kurly", "2003", Category.BEAUTY_SKINCARE, 20000L, 10);

        assertThat(ids(service.findSimilar(CURRENT)))
                .containsExactly("kurly-2001", "kurly-2002", "kurly-2003");
    }

    @Test
    void 가격을_모를_때도_공백뿐인_번호표는_LIMIT_전에_빠진다() {
        current(null);
        save(" ", " ", Category.BEAUTY_SKINCARE, 20000L, 900);
        save("kurly", " ", Category.BEAUTY_SKINCARE, 20000L, 800);
        save(" ", "2999", Category.BEAUTY_SKINCARE, 20000L, 700);
        save("kurly", "2001", Category.BEAUTY_SKINCARE, 100L, 30);
        save("kurly", "2002", Category.BEAUTY_SKINCARE, null, 20);
        save("kurly", "2003", Category.BEAUTY_SKINCARE, 999999L, 10);

        assertThat(ids(service.findSimilar(CURRENT)))
                .containsExactly("kurly-2001", "kurly-2002", "kurly-2003");
    }

    @Test
    void 탭이나_개행뿐인_번호표도_externalId_가_null_인_카드로_나가지_않는다() {
        // SQL TRIM 은 스페이스만 지우므로 이런 값은 쿼리를 통과할 수 있다. 결과에서는 반드시 빠져야 한다
        current(20000L);
        save("\t", "\t", Category.BEAUTY_SKINCARE, 20000L, 900);
        save("kurly", "\n", Category.BEAUTY_SKINCARE, 20000L, 800);
        save("kurly", "2001", Category.BEAUTY_SKINCARE, 20000L, 10);

        List<ChatRecommendation> recommendations = service.findSimilar(CURRENT);

        assertThat(recommendations).extracting(ChatRecommendation::externalId)
                .doesNotContainNull()
                .contains("kurly-2001");
    }

    @Test
    void 리뷰가_많은_순으로_최대_세_개를_고른다() {
        current(20000L);
        save("kurly", "r10", Category.BEAUTY_SKINCARE, 20000L, 10);
        save("kurly", "r300", Category.BEAUTY_SKINCARE, 20000L, 300);
        save("kurly", "r50", Category.BEAUTY_SKINCARE, 20000L, 50);
        save("oliveyoung", "r200", Category.BEAUTY_SKINCARE, 20000L, 200);

        assertThat(ids(service.findSimilar(CURRENT)))
                .containsExactly("kurly-r300", "oliveyoung-r200", "kurly-r50");
    }

    @Test
    void 리뷰_수가_null_인_상품은_뒤로_보낸다() {
        current(20000L);
        Product unknown = save("kurly", "unknown", Category.BEAUTY_SKINCARE, 20000L, 1);
        save("kurly", "r5", Category.BEAUTY_SKINCARE, 20000L, 5);
        // onCreate 가 null 을 0 으로 채우므로, 저장 뒤 null 로 되돌려 실제 null 행을 만든다
        unknown.setReviewCount(null);
        productRepository.saveAndFlush(unknown);

        assertThat(ids(service.findSimilar(CURRENT)))
                .containsExactly("kurly-r5", "kurly-unknown");
    }

    @Test
    void 상품을_못_찾으면_빈_목록이다() {
        save("kurly", "2001", Category.BEAUTY_SKINCARE, 20000L, 1);

        assertThat(service.findSimilar("kurly-404")).isEmpty();
    }

    @Test
    void 식별자가_없거나_형식이_아니면_빈_목록이다() {
        assertThat(service.findSimilar(null)).isEmpty();
        assertThat(service.findSimilar("900000000000")).isEmpty();
    }

    @Test
    void 카테고리가_없는_상품이면_빈_목록이다() {
        save("kurly", "1000", null, 20000L, 50);
        save("kurly", "2001", null, 20000L, 1);

        assertThat(service.findSimilar(CURRENT)).isEmpty();
    }

    @Test
    void 모르는_값은_0이_아니라_null_로_내보낸다() {
        current(20000L);
        save("kurly", "2001", Category.BEAUTY_SKINCARE, 20000L, null); // onCreate 가 0 / 0.0 으로 채운다

        ChatRecommendation recommendation = service.findSimilar(CURRENT).get(0);

        assertThat(recommendation.externalId()).isEqualTo("kurly-2001");
        assertThat(recommendation.platform()).isEqualTo("kurly");
        assertThat(recommendation.productId()).isEqualTo("2001");
        assertThat(recommendation.name()).isEqualTo("상품 2001");
        assertThat(recommendation.price()).isEqualTo(20000L);
        assertThat(recommendation.thumbnailUrl()).isEqualTo("https://img.example/2001.jpg");
        assertThat(recommendation.reviewCount()).isNull();
        assertThat(recommendation.rating()).isNull();
    }
}

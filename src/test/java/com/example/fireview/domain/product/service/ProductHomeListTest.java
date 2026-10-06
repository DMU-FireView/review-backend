package com.example.fireview.domain.product.service;

import com.example.fireview.domain.product.dto.ProductResponse;
import com.example.fireview.domain.product.entity.Category;
import com.example.fireview.domain.product.entity.Product;
import com.example.fireview.domain.product.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 홈 목록(GET /api/products)의 원천을 확인한다.
 * Data 서버 상품이 있으면 그것만, 없을 때만 더미를 보여준다.
 */
@DataJpaTest
@ActiveProfiles("test")
@Import(ProductService.class)
class ProductHomeListTest {

    @Autowired ProductService productService;
    @Autowired ProductRepository productRepository;

    private void dummy(long id) {
        productRepository.save(Product.builder().id(id).name("더미 " + id)
                .platform("NAVER").category(Category.DIGITAL_MOBILE).avgRti(80.0).build());
    }

    private void tag(long id, String productId) {
        productRepository.save(Product.builder().id(id).name("실상품 " + productId).platform("KURLY")
                .dataPlatform("kurly").dataProductId(productId).build());
    }

    @Test
    void Data_서버_상품이_없으면_더미를_보여준다() {
        dummy(900000000000L);
        dummy(900000000001L);

        assertThat(productService.getAllProducts()).hasSize(2);
    }

    @Test
    void Data_서버_상품이_하나라도_있으면_그것만_보여준다() {
        // 진짜와 가짜를 섞으면 사용자가 구분할 수 없다
        dummy(900000000000L);
        dummy(900000000001L);
        tag(1L, "1000146248");

        List<ProductResponse> home = productService.getAllProducts();

        assertThat(home).singleElement()
                .satisfies(p -> assertThat(p.externalId()).isEqualTo("kurly-1000146248"));
    }

    private static Product tagged(long id, Category category) {
        return Product.builder().id(id).name("p" + id).dataPlatform("kurly")
                .dataProductId(String.valueOf(id)).category(category).build();
    }

    @Test
    void 대분류별로_번갈아_고른다() {
        // 최근 순으로만 자르면 마지막 검색 키워드 몇 개가 홈을 다 차지한다
        List<Product> newestFirst = List.of(
                tagged(1, Category.BEAUTY_SKINCARE), tagged(2, Category.BEAUTY_MAKEUP),
                tagged(3, Category.BEAUTY_HAIR), tagged(4, Category.FOOD_PROCESSED),
                tagged(5, Category.FOOD_SNACK), tagged(6, null), tagged(7, Category.DIGITAL_AV));

        List<Product> mixed = ProductService.mixByMajorCategory(newestFirst, 100);

        // 뷰티 → 식품 → 미분류 → 디지털 → 뷰티 → 식품 → 뷰티
        assertThat(mixed).extracting(Product::getId).containsExactly(1L, 4L, 6L, 7L, 2L, 5L, 3L);
    }

    @Test
    void 정해진_수까지만_고른다() {
        List<Product> many = java.util.stream.LongStream.rangeClosed(1, 150)
                .mapToObj(i -> tagged(i, i % 2 == 0 ? Category.BEAUTY_SKINCARE : Category.FOOD_SNACK))
                .toList();

        assertThat(ProductService.mixByMajorCategory(many, 100)).hasSize(100);
        assertThat(ProductService.mixByMajorCategory(many.subList(0, 3), 100)).hasSize(3);
    }

    @Test
    void 홈은_최대_100건이다() {
        for (long i = 1; i <= 120; i++) tag(i, String.valueOf(1000 + i));

        assertThat(productService.getAllProducts()).hasSize(100);
    }
}

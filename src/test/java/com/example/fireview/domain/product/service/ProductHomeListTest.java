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
}

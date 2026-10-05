package com.example.fireview.domain.product.repository;

import com.example.fireview.domain.product.entity.Category;
import com.example.fireview.domain.product.entity.Product;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
class ProductDataServerKeyRepositoryTest {

    @Autowired ProductRepository productRepository;

    private Product product(long id, String platform, String productId) {
        return Product.builder()
                .id(id).name("샘플 상품").category(Category.DIGITAL_MOBILE)
                .platform("KURLY").avgRti(0.0)
                .dataPlatform(platform).dataProductId(productId)
                .build();
    }

    @Test
    void Data_서버_주소로_상품을_찾는다() {
        productRepository.save(product(1L, "kurly", "1000146248"));

        assertThat(productRepository.findByDataPlatformAndDataProductId("kurly", "1000146248"))
                .get().extracting(Product::getId).isEqualTo(1L);
    }

    @Test
    void 플랫폼이_다르면_다른_상품이다() {
        // 몰이 다른데 상품 ID 가 겹칠 수 있다. 플랫폼까지 봐야 구분된다
        productRepository.save(product(1L, "kurly", "123456"));
        productRepository.save(product(2L, "elevenst", "123456"));
        productRepository.flush();

        assertThat(productRepository.findByDataPlatformAndDataProductId("elevenst", "123456"))
                .get().extracting(Product::getId).isEqualTo(2L);
    }

    @Test
    void 같은_주소로_두_행을_만들_수_없다() {
        // 갈라지면 같은 상품인데 찜이 서로 다른 행에 붙는다
        productRepository.save(product(1L, "kurly", "1000146248"));
        productRepository.save(product(2L, "kurly", "1000146248"));

        assertThatThrownBy(() -> productRepository.flush())
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void 주소가_없는_행은_여러_개여도_된다() {
        // 기존 더미 33건이 전부 여기 해당한다. 유니크 제약에 걸리면 안 된다
        productRepository.save(product(900000000000L, null, null));
        productRepository.save(product(900000000001L, null, null));

        productRepository.flush();

        assertThat(productRepository.count()).isEqualTo(2);
    }
}

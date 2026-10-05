package com.example.fireview.domain.product.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProductDataServerAddressTest {

    private static Product with(String platform, String productId) {
        return Product.builder().id(1L).name("샘플").dataPlatform(platform).dataProductId(productId).build();
    }

    @Test
    void 두_칼럼이_다_있어야_주소로_본다() {
        assertThat(with("kurly", "1000146248").hasDataServerAddress()).isTrue();
        assertThat(with("kurly", null).hasDataServerAddress()).isFalse();
        assertThat(with(null, "1000146248").hasDataServerAddress()).isFalse();
        assertThat(with(null, null).hasDataServerAddress()).isFalse();
        assertThat(with("  ", "1000146248").hasDataServerAddress()).isFalse();
    }

    @Test
    void 외부_식별자는_하이픈으로_잇는다() {
        assertThat(with("kurly", "1000146248").dataServerExternalId()).isEqualTo("kurly-1000146248");
    }

    @Test
    void 주소가_없으면_외부_식별자도_없다() {
        assertThat(with(null, null).dataServerExternalId()).isNull();
    }

    @Test
    void 더미_상품은_주소가_없다() {
        // DataInitializer 가 만든 기존 33건. Data 서버에 대응하는 상품이 없다
        assertThat(Product.builder().id(900000000000L).name("삼성 갤럭시 S25 Ultra").build()
                .hasDataServerAddress()).isFalse();
    }
}

package com.example.fireview.domain.dataserver.service;

import com.example.fireview.domain.dataserver.DataServerProductKey;
import com.example.fireview.domain.product.entity.Product;
import com.example.fireview.domain.product.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 번호표 행이 실제로 저장되는지 확인한다.
 * category·avgRti 를 비운 채 저장할 수 있어야 하므로 NOT NULL 이 풀렸는지도 같이 걸린다.
 */
@DataJpaTest
@ActiveProfiles("test")
@Import(ProductTagRegistry.class)
class ProductTagRegistryJpaTest {

    private static final DataServerProductKey KEY =
            new DataServerProductKey("kurly", "1000146248");

    @Autowired ProductTagRegistry registry;
    @Autowired ProductRepository productRepository;

    @Test
    void 번호표를_만들고_같은_키로_다시_부르면_같은_행을_준다() {
        Product first = registry.resolveOrCreate(KEY, "토리든 마스크팩");
        Product second = registry.resolveOrCreate(KEY, "이름이 바뀌어도");

        assertThat(second.getId()).isEqualTo(first.getId());
        assertThat(productRepository.count()).isEqualTo(1);
    }

    @Test
    void 분석_전이라_카테고리와_RTI는_비어_있다() {
        // 0 이나 50 을 넣으면 화면이 그 수치를 실제 분석 결과처럼 보여준다
        Product tag = registry.resolveOrCreate(KEY, "토리든 마스크팩");

        assertThat(tag.getCategory()).isNull();
        assertThat(tag.getAvgRti()).isNull();
    }

    @Test
    void Data_서버_주소와_표시용_플랫폼이_함께_들어간다() {
        Product tag = registry.resolveOrCreate(KEY, "토리든 마스크팩");

        assertThat(tag.getDataPlatform()).isEqualTo("kurly");
        assertThat(tag.getDataProductId()).isEqualTo("1000146248");
        assertThat(tag.getPlatform()).isEqualTo("KURLY");
        assertThat(tag.dataServerExternalId()).isEqualTo("kurly-1000146248");
    }

    @Test
    void 상품명이_비면_외부_식별자를_쓴다() {
        // name 은 NOT NULL 이라 비울 수 없다. 저장 자체가 실패하면 찜을 못 건다
        Product tag = registry.resolveOrCreate(KEY, "  ");

        assertThat(tag.getName()).isEqualTo("kurly-1000146248");
    }
}

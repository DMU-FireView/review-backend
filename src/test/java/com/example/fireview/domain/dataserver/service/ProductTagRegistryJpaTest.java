package com.example.fireview.domain.dataserver.service;

import com.example.fireview.domain.dataserver.DataServerProductKey;
import com.example.fireview.domain.dataserver.dto.DataServerProduct;
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

    private static DataServerProduct searched(int price, String category) {
        return new DataServerProduct("kurly", "1000146248", "토리든 마스크팩",
                "https://www.kurly.com/goods/1000146248", "토리든", null, null,
                price, "https://img/1.jpg", category, 1318, null, null);
    }

    @Test
    void 검색_결과로_표시_정보를_채운다() {
        Product tag = registry.upsertForDisplay(searched(17000, "뷰티 > 스킨케어 > 마스크팩"));

        assertThat(tag.getName()).isEqualTo("토리든 마스크팩");
        assertThat(tag.getPrice()).isEqualTo(17000L);
        assertThat(tag.getImageUrl()).isEqualTo("https://img/1.jpg");
        assertThat(tag.getReviewCount()).isEqualTo(1318);
        // Data 서버 카테고리는 자유 문자열이라 enum 이 아닌 subCategory 에 그대로 둔다
        assertThat(tag.getSubCategory()).isEqualTo("뷰티 > 스킨케어 > 마스크팩");
        assertThat(tag.getCategory()).isNull();
        // 프론트의 "구매하러 가기"·최저가 표시가 이 링크를 쓴다
        assertThat(tag.getPlatformLinks()).singleElement()
                .satisfies(link -> {
                    assertThat(link.getPlatform()).isEqualTo("KURLY");
                    assertThat(link.getUrl()).isEqualTo("https://www.kurly.com/goods/1000146248");
                });
    }

    @Test
    void 다시_검색되면_같은_행을_최신값으로_덮는다() {
        Product first = registry.upsertForDisplay(searched(17000, "뷰티"));
        Product second = registry.upsertForDisplay(searched(15900, "뷰티"));

        assertThat(second.getId()).isEqualTo(first.getId());
        assertThat(second.getPrice()).isEqualTo(15900L);
        assertThat(second.getPlatformLinks()).hasSize(1);   // 링크가 쌓이지 않는다
        assertThat(productRepository.count()).isEqualTo(1);
    }

    @Test
    void 상세_응답이_값을_비워_와도_목록에_보이던_값을_지우지_않는다() {
        // 11번가·올리브영은 검색 응답에는 리뷰 수를 주고 상세 응답에서는 비운다.
        // 예전에는 상세를 한 번 열면 목록의 리뷰 수가 0 이 됐다.
        registry.upsertForDisplay(new DataServerProduct("kurly", "1000146248", "토리든 마스크팩",
                "https://www.kurly.com/goods/1000146248", null, null, null,
                17000, "https://img/1.jpg", "뷰티", 1318, 4.8, null));

        Product tag = registry.upsertForDisplay(new DataServerProduct("kurly", "1000146248",
                null, null, null, null, null, null, null, null, null, null, null));

        assertThat(tag.getName()).isEqualTo("토리든 마스크팩");
        assertThat(tag.getReviewCount()).isEqualTo(1318);
        assertThat(tag.getAvgRating()).isEqualTo(4.8);
        assertThat(tag.getPrice()).isEqualTo(17000L);
        assertThat(tag.getImageUrl()).isEqualTo("https://img/1.jpg");
        assertThat(tag.getSubCategory()).isEqualTo("뷰티");
        assertThat(tag.getPlatformLinks()).singleElement()
                .satisfies(link -> assertThat(link.getUrl())
                        .isEqualTo("https://www.kurly.com/goods/1000146248"));
    }

    @Test
    void 검색으로_만든_번호표도_분석_결과는_비어_있다() {
        Product tag = registry.upsertForDisplay(searched(17000, "뷰티"));

        assertThat(tag.getAvgRti()).isNull();
    }
}

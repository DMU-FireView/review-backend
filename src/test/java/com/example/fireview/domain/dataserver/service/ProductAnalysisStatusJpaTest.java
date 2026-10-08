package com.example.fireview.domain.dataserver.service;

import com.example.fireview.domain.dataserver.DataServerProductKey;
import com.example.fireview.domain.dataserver.dto.response.AnalysisStatus;
import com.example.fireview.domain.product.entity.Product;
import com.example.fireview.domain.product.repository.ProductRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * v2 상세에서 본 분석 상태를 번호표에 적는 부분(#205).
 * 목록·대시보드는 Data 서버를 부르지 않고 이 값을 그대로 보여준다.
 */
@DataJpaTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@ActiveProfiles("test")
@Import(ProductTagRegistry.class)
class ProductAnalysisStatusJpaTest {

    private static final DataServerProductKey KEY =
            new DataServerProductKey("kurly", "1000146248");

    @Autowired ProductTagRegistry registry;
    @Autowired ProductRepository productRepository;
    @Autowired EntityManager entityManager;
    @Autowired EntityManagerFactory entityManagerFactory;

    private Statistics statistics;

    @BeforeEach
    void setUp() {
        statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
    }

    private Product tag() {
        return registry.resolveOrCreate(KEY, "토리든 마스크팩");
    }

    /** 쓰기를 DB 까지 내보내고 영속성 컨텍스트를 비워, 다음 조회가 DB 값을 읽게 한다 */
    private Product reload(Product product) {
        entityManager.flush();
        entityManager.clear();
        return productRepository.findById(product.getId()).orElseThrow();
    }

    @Test
    void 새_번호표는_분석_상태를_모른다() {
        Product tag = reload(tag());

        assertThat(tag.getAnalysisStatus()).isNull();
        assertThat(tag.getAnalysisStatusAt()).isNull();
    }

    @Test
    void DONE을_보면_적는다() {
        Product tag = tag();

        boolean changed = registry.recordAnalysisStatus(KEY, AnalysisStatus.DONE);

        Product saved = reload(tag);
        assertThat(changed).isTrue();
        assertThat(saved.getAnalysisStatus()).isEqualTo(AnalysisStatus.DONE);
        assertThat(saved.getAnalysisStatusAt()).isNotNull();
    }

    @Test
    void QUEUED에서_DONE으로_바뀌면_갱신한다() {
        Product tag = tag();
        registry.recordAnalysisStatus(KEY, AnalysisStatus.QUEUED);
        assertThat(reload(tag).getAnalysisStatus()).isEqualTo(AnalysisStatus.QUEUED);

        boolean changed = registry.recordAnalysisStatus(KEY, AnalysisStatus.DONE);

        assertThat(changed).isTrue();
        assertThat(reload(tag).getAnalysisStatus()).isEqualTo(AnalysisStatus.DONE);
    }

    @Test
    void UNAVAILABLE은_적지_않고_이전_값을_지킨다() {
        // Data 서버에 잠깐 닿지 못한 것이 마지막으로 본 DONE 을 지우면 안 된다
        Product tag = tag();
        registry.recordAnalysisStatus(KEY, AnalysisStatus.DONE);
        LocalDateTime seenAt = reload(tag).getAnalysisStatusAt();

        boolean changed = registry.recordAnalysisStatus(KEY, AnalysisStatus.UNAVAILABLE);

        Product saved = reload(tag);
        assertThat(changed).isFalse();
        assertThat(saved.getAnalysisStatus()).isEqualTo(AnalysisStatus.DONE);
        assertThat(saved.getAnalysisStatusAt()).isEqualTo(seenAt);
    }

    @Test
    void 처음부터_UNAVAILABLE이면_null로_남는다() {
        Product tag = tag();

        registry.recordAnalysisStatus(KEY, AnalysisStatus.UNAVAILABLE);

        assertThat(reload(tag).getAnalysisStatus()).isNull();
    }

    @Test
    void 같은_상태를_다시_보면_UPDATE가_나가지_않는다() {
        Product tag = tag();
        registry.recordAnalysisStatus(KEY, AnalysisStatus.DONE);
        LocalDateTime seenAt = reload(tag).getAnalysisStatusAt();
        statistics.clear();

        boolean changed = registry.recordAnalysisStatus(KEY, AnalysisStatus.DONE);
        entityManager.flush();

        assertThat(changed).isFalse();
        assertThat(statistics.getEntityUpdateCount()).isZero();
        assertThat(reload(tag).getAnalysisStatusAt()).isEqualTo(seenAt);
    }

    @Test
    void 번호표가_없으면_만들지_않는다() {
        boolean changed = registry.recordAnalysisStatus(KEY, AnalysisStatus.DONE);

        assertThat(changed).isFalse();
        assertThat(productRepository.count()).isZero();
    }
}

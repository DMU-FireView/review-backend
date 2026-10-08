package com.example.fireview.domain.dataserver.service;

import com.example.fireview.domain.dataserver.DataServerProductKey;
import com.example.fireview.domain.dataserver.dto.DataServerProduct;
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
    void 같은_상태를_다시_보면_행을_바꾸지_않는다() {
        Product tag = tag();
        registry.recordAnalysisStatus(KEY, AnalysisStatus.DONE);
        LocalDateTime seenAt = reload(tag).getAnalysisStatusAt();
        statistics.clear();

        boolean changed = registry.recordAnalysisStatus(KEY, AnalysisStatus.DONE);
        entityManager.flush();

        // 칼럼 지정 UPDATE 의 WHERE 에서 걸러져 0 행이다. 엔티티 UPDATE 도 나가지 않는다
        assertThat(changed).isFalse();
        assertThat(statistics.getEntityUpdateCount()).isZero();
        assertThat(reload(tag).getAnalysisStatusAt()).isEqualTo(seenAt);
    }

    /** 다른 요청이 같은 행에 먼저 커밋한 것처럼, 영속성 컨텍스트를 거치지 않고 DB 값을 바꾼다 */
    private void writeBehindContext(String sql) {
        entityManager.flush();
        entityManager.createNativeQuery(sql).executeUpdate();
    }

    @Test
    void 상태_기록은_다른_칼럼을_건드리지_않는다() {
        // 이 트랜잭션은 옛 이름을 들고 있다. 그 사이 다른 요청이 이름을 바꿨다
        Product tag = tag();
        writeBehindContext("UPDATE products SET name = '새 이름', review_count = 77 WHERE id = " + tag.getId());

        registry.recordAnalysisStatus(KEY, AnalysisStatus.DONE);

        Product saved = reload(tag);
        assertThat(saved.getAnalysisStatus()).isEqualTo(AnalysisStatus.DONE);
        assertThat(saved.getName()).isEqualTo("새 이름");
        assertThat(saved.getReviewCount()).isEqualTo(77);
    }

    @Test
    void 다른_요청이_먼저_같은_상태를_적었으면_처음_본_시각을_지킨다() {
        // 이 트랜잭션이 읽어 둔 엔티티는 상태 null 이다. 같은 조건을 DB 에서 봐야 옛 값에 속지 않는다
        Product tag = tag();
        writeBehindContext("UPDATE products SET analysis_status = 'DONE', analysis_status_at = TIMESTAMP '2026-01-01 00:00:00'"
                + " WHERE id = " + tag.getId());

        boolean changed = registry.recordAnalysisStatus(KEY, AnalysisStatus.DONE);

        assertThat(changed).isFalse();
        assertThat(reload(tag).getAnalysisStatusAt()).isEqualTo(LocalDateTime.of(2026, 1, 1, 0, 0));
    }

    @Test
    void 표시_정보_저장이_동시에_기록된_분석_상태를_덮지_않는다() {
        // 검색 요청이 상태 null 인 행을 읽어 둔 사이 상세 요청이 DONE 을 커밋했다(#205 재현 순서)
        Product tag = tag();
        assertThat(tag.getAnalysisStatus()).isNull();
        writeBehindContext("UPDATE products SET analysis_status = 'DONE', analysis_status_at = CURRENT_TIMESTAMP"
                + " WHERE id = " + tag.getId());

        registry.upsertForDisplay(new DataServerProduct(
                "kurly", "1000146248", "바뀐 상품명", null, null, null, null, null, null, null, null, null, null));

        Product saved = reload(tag);
        assertThat(saved.getName()).isEqualTo("바뀐 상품명");
        assertThat(saved.getAnalysisStatus()).isEqualTo(AnalysisStatus.DONE);
        assertThat(saved.getAnalysisStatusAt()).isNotNull();
    }

    @Test
    void 모르는_저장값은_null로_읽고_새_상태로_덮을_수_있다() {
        // 새 버전이 적은 상태를 이 버전이 읽는 경우. 예외를 내면 목록 전체가 깨진다
        Product tag = tag();
        writeBehindContext("UPDATE products SET analysis_status = 'ARCHIVED' WHERE id = " + tag.getId());

        assertThat(reload(tag).getAnalysisStatus()).isNull();

        boolean changed = registry.recordAnalysisStatus(KEY, AnalysisStatus.DONE);

        assertThat(changed).isTrue();
        assertThat(reload(tag).getAnalysisStatus()).isEqualTo(AnalysisStatus.DONE);
    }

    @Test
    void 번호표가_없으면_만들지_않는다() {
        boolean changed = registry.recordAnalysisStatus(KEY, AnalysisStatus.DONE);

        assertThat(changed).isFalse();
        assertThat(productRepository.count()).isZero();
    }
}

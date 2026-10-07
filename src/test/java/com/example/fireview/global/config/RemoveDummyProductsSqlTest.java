package com.example.fireview.global.config;

import com.example.fireview.domain.cart.entity.CartItem;
import com.example.fireview.domain.dashboard.entity.ViewHistory;
import com.example.fireview.domain.feedback.entity.AnalysisFeedback;
import com.example.fireview.domain.feedback.entity.AnalysisFeedbackType;
import com.example.fireview.domain.product.entity.PlatformLink;
import com.example.fireview.domain.product.entity.Product;
import com.example.fireview.domain.report.entity.Report;
import com.example.fireview.domain.report.entity.ReportReason;
import com.example.fireview.domain.review.entity.FeedbackType;
import com.example.fireview.domain.review.entity.Review;
import com.example.fireview.domain.review.entity.ReviewFeedback;
import com.example.fireview.domain.review.entity.TrustGrade;
import com.example.fireview.domain.user.entity.OAuthProvider;
import com.example.fireview.domain.user.entity.User;
import com.example.fireview.domain.wishlist.entity.Wishlist;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * docs/sql/remove-dummy-products.sql 의 DELETE 순서가 엔티티가 만드는 FK 와 맞는지 확인한다.
 *
 * <p>운영은 PostgreSQL 이라 DO 블록(건수·구간 가드), SET LOCAL, LOCK TABLE ... IN ... MODE 는
 * H2 에서 돌릴 수 없다. 대상 임시 테이블을 만드는 문장과 DELETE 문만 파일에서 꺼내 파일에 적힌 순서대로
 * 실행한다. SET LOCAL 로 둔 구간 값은 current_setting(...) 자리에 그대로 넣는다.
 * 엔티티에 상품·리뷰를 가리키는 FK 가 새로 생기면, 그 테이블에 더미를 가리키는 행이 있을 때만
 * 여기서 FK 위반으로 실패한다. 새 테이블은 아래 픽스처에도 행을 넣어야 검출된다.
 */
@DataJpaTest
@ActiveProfiles("test")
class RemoveDummyProductsSqlTest {

    private static final Path SQL = Path.of("docs/sql/remove-dummy-products.sql");
    private static final Pattern TEMP_TABLE = Pattern.compile("^CREATE TEMP TABLE (\\w+)", Pattern.MULTILINE);

    private static final long DUMMY_ID_MIN = 900_000_000_000L;
    private static final long NAVER_CACHED_ID = 12_345_678_901L;

    @Autowired EntityManager em;

    /**
     * 커넥션 풀이 같은 세션을 다시 쓰면 앞 테스트의 임시 테이블이 남아 있다. DROP 도 H2 에선 커밋을 일으키므로
     * 픽스처를 넣기 전에 지운다.
     */
    @BeforeEach
    void dropTempTables() throws IOException {
        Matcher m = TEMP_TABLE.matcher(Files.readString(SQL, StandardCharsets.UTF_8));
        while (m.find()) {
            em.createNativeQuery("DROP TABLE IF EXISTS " + m.group(1)).executeUpdate();
        }
    }

    @Test
    void 더미_상품과_종속_행만_FK_위반_없이_지운다() throws IOException {
        User user = persist(User.builder().email("u@test.com").nickname("u")
                .provider(OAuthProvider.LOCAL).build());

        Product real = persist(Product.builder().id(1L).name("실상품").platform("KURLY")
                .dataPlatform("kurly").dataProductId("1000146248")
                .platformLinks(new ArrayList<>(List.of(new PlatformLink("KURLY", 1000L, "https://kurly")))).build());
        Product dummy = persist(Product.builder().id(900000000000L).name("더미").platform("NAVER")
                .platformLinks(new ArrayList<>(List.of(new PlatformLink("NAVER", 1000L, "https://naver")))).build());
        Product blank = persist(Product.builder().id(900000000001L).name("빈 번호표").platform("NAVER")
                .dataPlatform("").dataProductId("").build());
        // 탭·개행·전각 공백만 있는 번호표도 String.isBlank() 처럼 번호표 없음으로 본다
        persist(Product.builder().id(900000000002L).name("탭 번호표").platform("NAVER")
                .dataPlatform("\t").dataProductId("1").build());
        persist(Product.builder().id(900000000003L).name("개행 번호표").platform("NAVER")
                .dataPlatform("naver").dataProductId(" \n\r\u3000").build());
        // 네이버 캐시로 저장된 실제 상품: 번호표가 없지만 더미 구간 밖이라 남아야 한다
        Product naverCached = persist(Product.builder().id(NAVER_CACHED_ID).name("네이버 캐시").platform("NAVER")
                .naverProductId(String.valueOf(NAVER_CACHED_ID)).build());

        Review realReview = persist(review(real));
        Review dummyReview = persist(review(dummy));

        // 더미 리뷰·상품에 붙은 사용자 데이터
        persist(AnalysisFeedback.builder().submitter(user).review(dummyReview)
                .feedbackType(AnalysisFeedbackType.SCORE_MISMATCH)
                .relatedSignals(new ArrayList<>(List.of("repetition"))).build());
        persist(ReviewFeedback.builder().review(dummyReview).user(user).feedbackType(FeedbackType.REAL).build());
        persist(ReviewFeedback.builder().product(blank).externalReviewId("ext-1").user(user)
                .feedbackType(FeedbackType.REAL).build());
        persist(Report.builder().reporter(user).review(dummyReview).reason(ReportReason.FAKE_REVIEW).build());
        persist(Report.builder().reporter(user).product(dummy).externalReviewId("ext-2")
                .reason(ReportReason.OTHER).build());
        persist(Wishlist.builder().user(user).product(dummy).build());
        persist(CartItem.builder().user(user).product(dummy).build());
        persist(ViewHistory.builder().user(user).product(dummy).build());

        // 실제 상품에 붙은 행은 남아야 한다
        persist(Wishlist.builder().user(user).product(real).build());
        persist(Report.builder().reporter(user).review(realReview).reason(ReportReason.OTHER).build());
        Review naverCachedReview = persist(review(naverCached));
        persist(Wishlist.builder().user(user).product(naverCached).build());
        persist(Report.builder().reporter(user).review(naverCachedReview).reason(ReportReason.OTHER).build());
        em.flush();
        em.clear();

        runStatements();
        assertThat(ids("SELECT id FROM ticketed_in_range")).isEmpty();
        assertThat(ids("SELECT id FROM dummy_products ORDER BY id"))
                .containsExactly(DUMMY_ID_MIN, DUMMY_ID_MIN + 1, DUMMY_ID_MIN + 2, DUMMY_ID_MIN + 3);

        assertThat(ids("SELECT id FROM products ORDER BY id")).containsExactly(1L, NAVER_CACHED_ID);
        assertThat(count("SELECT COUNT(*) FROM reviews")).isEqualTo(2);
        assertThat(count("SELECT COUNT(*) FROM review_reasons")).isEqualTo(2);
        assertThat(count("SELECT COUNT(*) FROM product_platform_links")).isEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM analysis_feedbacks")).isZero();
        assertThat(count("SELECT COUNT(*) FROM analysis_feedback_signals")).isZero();
        assertThat(count("SELECT COUNT(*) FROM review_feedbacks")).isZero();
        assertThat(count("SELECT COUNT(*) FROM reports")).isEqualTo(2);
        assertThat(count("SELECT COUNT(*) FROM wishlists")).isEqualTo(2);
        assertThat(count("SELECT COUNT(*) FROM cart_items")).isZero();
        assertThat(count("SELECT COUNT(*) FROM view_histories")).isZero();
        assertThat(count("SELECT COUNT(*) FROM users")).isEqualTo(1);
    }

    @Test
    void 더미_구간_안에_번호표가_있는_상품이_있으면_가드에_걸리고_대상에서_빠진다() throws IOException {
        persist(Product.builder().id(DUMMY_ID_MIN).name("더미").platform("NAVER").build());
        persist(Product.builder().id(DUMMY_ID_MIN + 1).name("구간 안 실상품").platform("KURLY")
                .dataPlatform("kurly").dataProductId("promoted-real").build());
        em.flush();
        em.clear();

        runStatements(statement -> !statement.startsWith("DELETE"));

        assertThat(ids("SELECT id FROM ticketed_in_range")).containsExactly(DUMMY_ID_MIN + 1);
        assertThat(ids("SELECT id FROM dummy_products")).containsExactly(DUMMY_ID_MIN);
        // ticketed_in_range 가 비어 있지 않으면 DO 블록이 DELETE 전에 중단한다(PL/pgSQL 이라 H2 에선 실행 불가)
        String sql = Files.readString(SQL, StandardCharsets.UTF_8);
        assertThat(sql.indexOf("SELECT COUNT(*) INTO ticketed FROM ticketed_in_range"))
                .isPositive()
                .isLessThan(sql.indexOf("DELETE FROM"));
        assertThat(sql).contains("IF ticketed > 0 THEN\n        RAISE EXCEPTION");
    }

    private void runStatements() throws IOException {
        runStatements(statement -> true);
    }

    private void runStatements(Predicate<String> filter) throws IOException {
        for (String statement : statementsToRun()) {
            if (filter.test(statement)) {
                em.createNativeQuery(statement).executeUpdate();
            }
        }
        em.clear();
    }

    /**
     * 임시 테이블 생성문과 DELETE 문을 파일 순서대로.
     * ON COMMIT DROP 은 H2 문법과 달라 뺀다. H2 에서 DDL 은 열린 트랜잭션을 커밋해 픽스처가 다른 테스트로 새므로,
     * 커밋하지 않는 세션 한정 임시 테이블(LOCAL TEMPORARY ... TRANSACTIONAL)로 바꾼다.
     */
    private static List<String> statementsToRun() throws IOException {
        String sql = Files.readString(SQL, StandardCharsets.UTF_8);
        Map<String, String> settings = settings(sql);
        Matcher m = Pattern.compile("(?s)^\\s*(CREATE TEMP TABLE .*?|DELETE FROM .*?);", Pattern.MULTILINE)
                .matcher(sql);
        List<String> out = new ArrayList<>();
        while (m.find()) {
            String statement = m.group(1).replace(" ON COMMIT DROP", "")
                    .replace("CREATE TEMP TABLE", "CREATE LOCAL TEMPORARY TABLE")
                    .replaceFirst("^(CREATE LOCAL TEMPORARY TABLE \\w+)", "$1 TRANSACTIONAL");
            for (Map.Entry<String, String> e : settings.entrySet()) {
                statement = statement.replace("current_setting('" + e.getKey() + "')::BIGINT", e.getValue());
            }
            assertThat(statement).as("H2 로 옮기지 못한 설정값").doesNotContain("current_setting");
            out.add(statement);
        }
        assertThat(out).as("임시 테이블 + DELETE 문").anyMatch(s -> s.startsWith("DELETE"));
        return out;
    }

    /** SET LOCAL fireview.xxx = '숫자'; 로 둔 설정값 */
    private static Map<String, String> settings(String sql) {
        Matcher m = Pattern.compile("^SET LOCAL (fireview\\.\\w+) = '(\\d+)';", Pattern.MULTILINE).matcher(sql);
        Map<String, String> out = new HashMap<>();
        while (m.find()) {
            out.put(m.group(1), m.group(2));
        }
        return out;
    }

    private static Review review(Product product) {
        return Review.builder().product(product).reviewerNickname("n").reviewerId("r")
                .rtiScore(30.0).trustGrade(TrustGrade.DANGER).writtenAt(LocalDateTime.now())
                .reasons(new ArrayList<>(List.of("반복 표현"))).build();
    }

    private <T> T persist(T entity) {
        em.persist(entity);
        return entity;
    }

    private long count(String sql) {
        return ((Number) em.createNativeQuery(sql).getSingleResult()).longValue();
    }

    @SuppressWarnings("unchecked")
    private List<Long> ids(String sql) {
        return ((List<Number>) em.createNativeQuery(sql).getResultList()).stream()
                .map(Number::longValue).toList();
    }
}

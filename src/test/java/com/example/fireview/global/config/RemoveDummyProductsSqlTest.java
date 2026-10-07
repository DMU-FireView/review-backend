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
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * docs/sql/remove-dummy-products.sql 의 DELETE 순서가 엔티티가 만드는 FK 와 맞는지 확인한다.
 *
 * <p>운영은 PostgreSQL 이라 DO 블록 자체는 H2 에서 돌릴 수 없다. 대상 임시 테이블을 만드는 문장과
 * DELETE 문만 파일에서 꺼내 파일에 적힌 순서대로 실행한다. 엔티티에 상품·리뷰를 가리키는 FK 가
 * 새로 생기면 여기서 FK 위반으로 실패하므로 SQL 도 같이 고쳐야 한다.
 */
@DataJpaTest
@ActiveProfiles("test")
class RemoveDummyProductsSqlTest {

    private static final Path SQL = Path.of("docs/sql/remove-dummy-products.sql");

    @Autowired EntityManager em;

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
        em.flush();
        em.clear();

        for (String statement : statementsToRun()) {
            em.createNativeQuery(statement).executeUpdate();
        }
        em.clear();

        assertThat(ids("SELECT id FROM products")).containsExactly(1L);
        assertThat(count("SELECT COUNT(*) FROM reviews")).isEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM review_reasons")).isEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM product_platform_links")).isEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM analysis_feedbacks")).isZero();
        assertThat(count("SELECT COUNT(*) FROM analysis_feedback_signals")).isZero();
        assertThat(count("SELECT COUNT(*) FROM review_feedbacks")).isZero();
        assertThat(count("SELECT COUNT(*) FROM reports")).isEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM wishlists")).isEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM cart_items")).isZero();
        assertThat(count("SELECT COUNT(*) FROM view_histories")).isZero();
        assertThat(count("SELECT COUNT(*) FROM users")).isEqualTo(1);
    }

    /** 임시 테이블 생성문과 DELETE 문을 파일 순서대로. ON COMMIT DROP 은 H2 문법과 달라 뺀다 */
    private static List<String> statementsToRun() throws IOException {
        String sql = Files.readString(SQL, StandardCharsets.UTF_8);
        Matcher m = Pattern.compile("(?s)^\\s*(CREATE TEMP TABLE .*?|DELETE FROM .*?);", Pattern.MULTILINE)
                .matcher(sql);
        List<String> out = new ArrayList<>();
        while (m.find()) {
            out.add(m.group(1).replace(" ON COMMIT DROP", ""));
        }
        assertThat(out).as("임시 테이블 2개 + DELETE 문").hasSizeGreaterThan(2);
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

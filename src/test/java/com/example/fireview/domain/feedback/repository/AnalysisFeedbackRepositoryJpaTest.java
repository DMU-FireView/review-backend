package com.example.fireview.domain.feedback.repository;

import com.example.fireview.domain.feedback.dto.response.AnalysisFeedbackResponse;
import com.example.fireview.domain.feedback.dto.response.UnifiedFeedbackResponse;
import com.example.fireview.domain.feedback.entity.AnalysisFeedback;
import com.example.fireview.domain.feedback.entity.AnalysisFeedbackStatus;
import com.example.fireview.domain.feedback.entity.AnalysisFeedbackType;
import com.example.fireview.domain.feedback.service.FeedbackStatusService;
import com.example.fireview.domain.product.entity.Product;
import com.example.fireview.domain.review.entity.Review;
import com.example.fireview.domain.review.entity.TrustGrade;
import com.example.fireview.domain.user.entity.OAuthProvider;
import com.example.fireview.domain.user.entity.User;
import com.example.fireview.domain.user.service.UserService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * 내역·통합·관리자 목록이 Spring 리뷰 대상과 Data 서버 리뷰 대상(review_id = null) 행을 모두 보여 주는지 본다.
 * INNER JOIN 이면 외부 행이 조용히 빠지고 total 도 틀린다. DTO 단위 테스트로는 잡히지 않는다.
 */
@DataJpaTest
@ActiveProfiles("test")
@Import(FeedbackStatusService.class)
class AnalysisFeedbackRepositoryJpaTest {

    private static final PageRequest NEWEST_FIRST = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));

    @Autowired AnalysisFeedbackRepository repository;
    @Autowired FeedbackStatusService feedbackStatusService;
    @Autowired EntityManager em;
    @MockitoBean UserService userService;

    private User me;
    private User other;
    private Product tag;
    private Review review;

    @BeforeEach
    void setUp() {
        me = persist(User.builder().email("me@test.com").nickname("me").provider(OAuthProvider.LOCAL).build());
        other = persist(User.builder().email("other@test.com").nickname("other").provider(OAuthProvider.LOCAL).build());
        tag = persist(Product.builder().id(42L).name("토리든 마스크팩").platform("KURLY")
                .dataPlatform("kurly").dataProductId("1000146248").build());
        Product legacy = persist(Product.builder().id(1L).name("기존 상품").platform("NAVER").build());
        review = persist(Review.builder().product(legacy).content("광고 같아요").reviewerNickname("n").reviewerId("r")
                .rtiScore(30.0).trustGrade(TrustGrade.DANGER).writtenAt(LocalDateTime.now())
                .reasons(new ArrayList<>(List.of("반복 표현"))).build());
    }

    private AnalysisFeedback internal(User submitter, AnalysisFeedbackStatus status) {
        return persist(AnalysisFeedback.builder().submitter(submitter).review(review)
                .feedbackType(AnalysisFeedbackType.SCORE_MISMATCH).status(status)
                .relatedSignals(new ArrayList<>(List.of("repetition"))).build());
    }

    private AnalysisFeedback external(User submitter, String externalReviewId, AnalysisFeedbackStatus status) {
        return persist(AnalysisFeedback.builder().submitter(submitter).product(tag).externalReviewId(externalReviewId)
                .feedbackType(AnalysisFeedbackType.EXPLANATION_INSUFFICIENT).status(status).build());
    }

    private void flushAndClear() {
        em.flush();
        em.clear();
    }

    @Test
    void 내_내역은_내부와_외부_행을_모두_돌려준다() {
        internal(me, AnalysisFeedbackStatus.SUBMITTED);
        external(me, "r-1", AnalysisFeedbackStatus.SUBMITTED);
        external(me, "r-2", AnalysisFeedbackStatus.RESOLVED);
        external(other, "r-3", AnalysisFeedbackStatus.SUBMITTED);
        flushAndClear();

        Page<AnalysisFeedbackResponse> page = repository.findBySubmitterIdWithReview(me.getId(), NEWEST_FIRST)
                .map(AnalysisFeedbackResponse::from);

        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getContent()).extracting(AnalysisFeedbackResponse::externalReviewId)
                .containsExactlyInAnyOrder(null, "r-1", "r-2");
        assertThat(page.getContent()).filteredOn(r -> r.externalReviewId() != null)
                .allSatisfy(r -> {
                    assertThat(r.reviewId()).isNull();
                    assertThat(r.reviewContent()).isNull();
                    assertThat(r.productName()).isEqualTo("토리든 마스크팩");
                    assertThat(r.productExternalId()).isEqualTo("kurly-1000146248");
                });
        assertThat(page.getContent()).filteredOn(r -> r.externalReviewId() == null)
                .singleElement()
                .satisfies(r -> {
                    assertThat(r.reviewId()).isEqualTo(review.getId());
                    assertThat(r.reviewContent()).isEqualTo("광고 같아요");
                    assertThat(r.productName()).isEqualTo("기존 상품");
                });
    }

    @Test
    void 혼합_목록을_나눠_받아도_total_과_페이지가_맞다() {
        internal(me, AnalysisFeedbackStatus.SUBMITTED);
        internal(me, AnalysisFeedbackStatus.SUBMITTED);
        external(me, "r-1", AnalysisFeedbackStatus.SUBMITTED);
        external(me, "r-2", AnalysisFeedbackStatus.SUBMITTED);
        external(me, "r-3", AnalysisFeedbackStatus.SUBMITTED);
        flushAndClear();

        Page<AnalysisFeedback> first = repository.findBySubmitterIdWithReview(me.getId(),
                PageRequest.of(0, 3, Sort.by(Sort.Direction.DESC, "createdAt")));
        Page<AnalysisFeedback> second = repository.findBySubmitterIdWithReview(me.getId(),
                PageRequest.of(1, 3, Sort.by(Sort.Direction.DESC, "createdAt")));

        assertThat(first.getTotalElements()).isEqualTo(5);
        assertThat(first.getTotalPages()).isEqualTo(2);
        assertThat(first.getContent()).hasSize(3);
        assertThat(second.getContent()).hasSize(2);
        assertThat(Stream.concat(first.stream(), second.stream()).map(AnalysisFeedback::getId).distinct())
                .hasSize(5);
    }

    @Test
    void 관리자_전체_목록은_외부_행도_보여준다() {
        internal(me, AnalysisFeedbackStatus.SUBMITTED);
        external(me, "r-1", AnalysisFeedbackStatus.UNDER_REVIEW);
        external(other, "r-2", AnalysisFeedbackStatus.SUBMITTED);
        flushAndClear();

        Page<AnalysisFeedbackResponse> all = repository.findAllWithDetails(NEWEST_FIRST)
                .map(AnalysisFeedbackResponse::from);

        assertThat(all.getTotalElements()).isEqualTo(3);
        assertThat(all.getContent()).extracting(AnalysisFeedbackResponse::externalReviewId)
                .containsExactlyInAnyOrder(null, "r-1", "r-2");
    }

    @Test
    void 관리자_상태별_목록도_외부_행을_보여준다() {
        internal(me, AnalysisFeedbackStatus.SUBMITTED);
        external(me, "r-1", AnalysisFeedbackStatus.SUBMITTED);
        external(other, "r-2", AnalysisFeedbackStatus.REJECTED);
        flushAndClear();

        Page<AnalysisFeedbackResponse> submitted = repository.findByStatus(AnalysisFeedbackStatus.SUBMITTED,
                NEWEST_FIRST).map(AnalysisFeedbackResponse::from);

        assertThat(submitted.getTotalElements()).isEqualTo(2);
        assertThat(submitted.getContent()).extracting(AnalysisFeedbackResponse::externalReviewId)
                .containsExactlyInAnyOrder(null, "r-1");
        assertThat(repository.countByStatus(AnalysisFeedbackStatus.SUBMITTED)).isEqualTo(2);
    }

    @Test
    void 통합_내역은_외부_분석_피드백을_본문_없이_포함한다() {
        when(userService.findByEmail("me@test.com")).thenReturn(me);
        internal(me, AnalysisFeedbackStatus.SUBMITTED);
        external(me, "r-1", AnalysisFeedbackStatus.RESOLVED);
        flushAndClear();

        List<UnifiedFeedbackResponse> unified = feedbackStatusService.getUnifiedFeedbacks("me@test.com");

        assertThat(unified).hasSize(2).allSatisfy(u -> {
            assertThat(u.feedbackCategory()).isEqualTo("ANALYSIS_FEEDBACK");
            assertThat(u.productName()).isNotNull();
        });
        assertThat(unified).extracting(UnifiedFeedbackResponse::reviewContent)
                .containsExactlyInAnyOrder("광고 같아요", null);
    }

    @Test
    void 대상이_둘_다_있거나_둘_다_없으면_저장되지_않는다() {
        AnalysisFeedback both = AnalysisFeedback.builder().submitter(me).review(review).product(tag)
                .externalReviewId("r-1").feedbackType(AnalysisFeedbackType.SCORE_MISMATCH).build();
        AnalysisFeedback none = AnalysisFeedback.builder().submitter(me)
                .feedbackType(AnalysisFeedbackType.SCORE_MISMATCH).build();

        assertThatThrownBy(() -> em.persist(both)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> em.persist(none)).isInstanceOf(IllegalStateException.class);
    }

    private <T> T persist(T entity) {
        em.persist(entity);
        return entity;
    }
}

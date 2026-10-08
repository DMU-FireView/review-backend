package com.example.fireview.domain.feedback.entity;

import com.example.fireview.domain.feedback.dto.response.AnalysisFeedbackResponse;
import com.example.fireview.domain.feedback.dto.response.UnifiedFeedbackResponse;
import com.example.fireview.domain.product.entity.Product;
import com.example.fireview.domain.review.entity.Review;
import com.example.fireview.domain.review.entity.TrustGrade;
import com.example.fireview.domain.user.entity.User;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 분석 피드백 대상은 Spring 리뷰와 (상품 번호표 + 외부 리뷰 ID) 중 정확히 하나다.
 * Data 서버 리뷰 피드백은 Spring DB 에 리뷰 행이 없으므로, 그 상태로 응답 변환이 터지지 않는지도 본다.
 */
class AnalysisFeedbackTargetTest {

    private static Product tag() {
        return Product.builder().id(42L).name("토리든 마스크팩").platform("KURLY")
                .dataPlatform("kurly").dataProductId("1000146248").build();
    }

    private static Review review() {
        return Review.builder().id(5L).content("광고 같아요").rtiScore(30.0).trustGrade(TrustGrade.DANGER)
                .product(Product.builder().id(1L).name("기존 상품").build())
                .writtenAt(LocalDateTime.now()).reviewerNickname("익명").reviewerId("u1").build();
    }

    private static AnalysisFeedback.AnalysisFeedbackBuilder base() {
        return AnalysisFeedback.builder().id(7L)
                .submitter(User.builder().id(1L).email("u@test.com").nickname("tester").build())
                .feedbackType(AnalysisFeedbackType.SCORE_MISMATCH)
                .status(AnalysisFeedbackStatus.SUBMITTED)
                .createdAt(LocalDateTime.now());
    }

    @Test
    void 둘_다_채우면_거절한다() {
        AnalysisFeedback both = base().review(review()).product(tag()).externalReviewId("r-99").build();

        assertThatThrownBy(both::validateTarget).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void Spring_리뷰에_외부_리뷰_ID만_섞여도_거절한다() {
        assertThatThrownBy(base().review(review()).externalReviewId("r-99").build()::validateTarget)
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void 둘_다_비면_거절한다() {
        assertThatThrownBy(base().build()::validateTarget).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void 외부_경로의_한쪽만_있으면_거절한다() {
        assertThatThrownBy(base().product(tag()).build()::validateTarget)
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(base().externalReviewId("r-99").build()::validateTarget)
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(base().product(tag()).externalReviewId("  ").build()::validateTarget)
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void 외부_리뷰_ID가_칼럼_길이를_넘으면_거절한다() {
        String tooLong = "r".repeat(AnalysisFeedback.EXTERNAL_REVIEW_ID_MAX_LENGTH + 1);

        assertThatThrownBy(base().product(tag()).externalReviewId(tooLong).build()::validateTarget)
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void 한쪽만_채우면_통과한다() {
        assertThatCode(base().review(review()).build()::validateTarget).doesNotThrowAnyException();
        assertThatCode(base().product(tag()).externalReviewId("r-99").build()::validateTarget)
                .doesNotThrowAnyException();
    }

    @Test
    void 외부_리뷰_피드백은_본문_없이_외부_식별자로_변환된다() {
        AnalysisFeedback external = base().product(tag()).externalReviewId("r-99").build();

        AnalysisFeedbackResponse res = AnalysisFeedbackResponse.from(external);
        assertThat(res.reviewId()).isNull();
        assertThat(res.reviewContent()).isNull();
        assertThat(res.productName()).isEqualTo("토리든 마스크팩");
        assertThat(res.externalReviewId()).isEqualTo("r-99");
        assertThat(res.productExternalId()).isEqualTo("kurly-1000146248");

        UnifiedFeedbackResponse unified = UnifiedFeedbackResponse.fromAnalysisFeedback(external);
        assertThat(unified.feedbackCategory()).isEqualTo("ANALYSIS_FEEDBACK");
        assertThat(unified.productName()).isEqualTo("토리든 마스크팩");
        assertThat(unified.reviewContent()).isNull();
        assertThat(unified.currentStep()).isEqualTo(1);
    }

    @Test
    void 기존_Spring_리뷰_피드백은_그대로_변환된다() {
        AnalysisFeedback internal = base().review(review()).build();

        AnalysisFeedbackResponse res = AnalysisFeedbackResponse.from(internal);
        assertThat(res.reviewId()).isEqualTo(5L);
        assertThat(res.reviewContent()).isEqualTo("광고 같아요");
        assertThat(res.productName()).isEqualTo("기존 상품");
        assertThat(res.externalReviewId()).isNull();
        assertThat(res.productExternalId()).isNull();

        UnifiedFeedbackResponse unified = UnifiedFeedbackResponse.fromAnalysisFeedback(internal);
        assertThat(unified.productName()).isEqualTo("기존 상품");
        assertThat(unified.reviewContent()).isEqualTo("광고 같아요");
    }
}

package com.example.fireview.domain.report.dto;

import com.example.fireview.domain.product.entity.Product;
import com.example.fireview.domain.report.dto.response.ReportResponse;
import com.example.fireview.domain.report.dto.response.ReportSummaryResponse;
import com.example.fireview.domain.report.entity.Report;
import com.example.fireview.domain.report.entity.ReportReason;
import com.example.fireview.domain.report.entity.ReportStatus;
import com.example.fireview.domain.review.dto.FeedbackHistoryResponse;
import com.example.fireview.domain.review.entity.FeedbackType;
import com.example.fireview.domain.review.entity.Review;
import com.example.fireview.domain.review.entity.ReviewFeedback;
import com.example.fireview.domain.review.entity.TrustGrade;
import com.example.fireview.domain.user.entity.User;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Data 서버 리뷰 신고·피드백은 Spring DB 에 리뷰 행이 없다.
 * 그 상태로 목록을 조회할 때 터지지 않는지 본다.
 */
class ExternalReviewReportDtoTest {

    private static Product tag() {
        return Product.builder().id(42L).name("토리든 마스크팩").platform("KURLY")
                .dataPlatform("kurly").dataProductId("1000146248").build();
    }

    private static User user() {
        return User.builder().id(1L).email("u@test.com").nickname("tester").build();
    }

    private static Report externalReport() {
        return Report.builder()
                .id(7L).reporter(user()).product(tag()).externalReviewId("r-99")
                .reason(ReportReason.AD_REVIEW).status(ReportStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void 외부_리뷰_신고도_응답으로_변환된다() {
        assertThatCode(() -> ReportResponse.from(externalReport())).doesNotThrowAnyException();
        assertThatCode(() -> ReportSummaryResponse.from(externalReport())).doesNotThrowAnyException();
    }

    @Test
    void 리뷰_본문은_없고_외부_식별자로_찾아간다() {
        // 본문을 복사해 두면 신고 내용을 위조할 수 있다. 운영자는 상품으로 들어가 확인한다
        ReportResponse res = ReportResponse.from(externalReport());

        assertThat(res.reviewId()).isNull();
        assertThat(res.reviewContent()).isNull();
        assertThat(res.externalReviewId()).isEqualTo("r-99");
        assertThat(res.productExternalId()).isEqualTo("kurly-1000146248");
        assertThat(res.productName()).isEqualTo("토리든 마스크팩");
    }

    @Test
    void 기존_Spring_리뷰_신고는_그대로_동작한다() {
        Review review = Review.builder()
                .id(5L).content("광고 같아요").rtiScore(30.0).trustGrade(TrustGrade.DANGER)
                .product(Product.builder().id(1L).name("기존 상품").build())
                .writtenAt(LocalDateTime.now()).reviewerNickname("익명").reviewerId("u1")
                .build();
        Report report = Report.builder()
                .id(8L).reporter(user()).review(review)
                .reason(ReportReason.AD_REVIEW).status(ReportStatus.PENDING)
                .createdAt(LocalDateTime.now()).build();

        ReportResponse res = ReportResponse.from(report);

        assertThat(res.reviewId()).isEqualTo(5L);
        assertThat(res.reviewContent()).isEqualTo("광고 같아요");
        assertThat(res.productName()).isEqualTo("기존 상품");
        assertThat(res.externalReviewId()).isNull();
    }

    @Test
    void 외부_리뷰_피드백_내역도_변환된다() {
        ReviewFeedback feedback = ReviewFeedback.builder()
                .id(3L).user(user()).product(tag()).externalReviewId("r-99")
                .feedbackType(FeedbackType.FAKE).createdAt(LocalDateTime.now())
                .build();

        assertThatCode(() -> FeedbackHistoryResponse.from(feedback)).doesNotThrowAnyException();

        FeedbackHistoryResponse res = FeedbackHistoryResponse.from(feedback);
        assertThat(res.reviewId()).isNull();
        assertThat(res.productId()).isEqualTo(42L);
        assertThat(res.productName()).isEqualTo("토리든 마스크팩");
    }
}

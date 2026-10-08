package com.example.fireview.domain.report.repository;

import com.example.fireview.domain.admin.service.AdminService;
import com.example.fireview.domain.feedback.dto.response.UnifiedFeedbackResponse;
import com.example.fireview.domain.feedback.repository.AnalysisFeedbackRepository;
import com.example.fireview.domain.feedback.service.FeedbackStatusService;
import com.example.fireview.domain.product.entity.Product;
import com.example.fireview.domain.report.dto.response.ReportResponse;
import com.example.fireview.domain.report.dto.response.ReportSummaryResponse;
import com.example.fireview.domain.report.entity.Report;
import com.example.fireview.domain.report.entity.ReportReason;
import com.example.fireview.domain.report.entity.ReportStatus;
import com.example.fireview.domain.report.service.ReportService;
import com.example.fireview.domain.review.dto.FeedbackHistoryResponse;
import com.example.fireview.domain.review.entity.FeedbackType;
import com.example.fireview.domain.review.entity.Review;
import com.example.fireview.domain.review.entity.ReviewFeedback;
import com.example.fireview.domain.review.entity.TrustGrade;
import com.example.fireview.domain.review.repository.ReviewFeedbackRepository;
import com.example.fireview.domain.review.service.ReviewService;
import com.example.fireview.domain.user.entity.OAuthProvider;
import com.example.fireview.domain.user.entity.User;
import com.example.fireview.domain.user.service.UserService;
import com.example.fireview.global.exception.CustomException;
import com.example.fireview.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@DataJpaTest
@ActiveProfiles("test")
class ExternalFeedbackHistoryJpaTest {
    @Autowired TestEntityManager em;
    @Autowired ReportRepository reports;
    @Autowired ReviewFeedbackRepository feedbacks;
    @Autowired AnalysisFeedbackRepository analyses;
    private final List<Long> reportIds = new ArrayList<>();
    private final List<Long> feedbackIds = new ArrayList<>();
    private User owner;
    private User other;
    private Product internalProduct;
    private Product externalProduct;
    private ReportService reportService;
    private ReviewService reviewService;
    private AdminService adminService;
    private FeedbackStatusService unifiedService;

    @BeforeEach
    void setUp() {
        owner = em.persist(User.builder().email("owner@test.com").nickname("owner")
                .provider(OAuthProvider.LOCAL).build());
        other = em.persist(User.builder().email("other@test.com").nickname("other")
                .provider(OAuthProvider.LOCAL).build());
        internalProduct = em.persist(Product.builder().id(1L).name("내부 상품").build());
        externalProduct = em.persist(Product.builder().id(2L).name("외부 상품")
                .dataPlatform("kurly").dataProductId("p-2").build());
        // 사용자 인증 조회만 대체하고 실제 repository + service + DTO를 검증한다.
        UserService users = mock(UserService.class);
        when(users.findByEmail("owner@test.com")).thenReturn(owner);
        when(users.findByEmail("other@test.com")).thenReturn(other);
        reportService = new ReportService(reports, null, null, users, null);
        reviewService = new ReviewService(null, feedbacks, null, null, users, null);
        adminService = new AdminService(reports, reportService, null, feedbacks, analyses, null, null);
        unifiedService = new FeedbackStatusService(reports, analyses, users);
    }

    @ParameterizedTest
    @ValueSource(strings = {"internal", "external", "mixed"})
    void myReportPagesRetainEveryTargetAndCount(String targets) {
        seed(targets);
        Page<ReportSummaryResponse> first = reportService.getMyReports("owner@test.com", PageRequest.of(0, 2));
        assertThat(first.getContent()).extracting(ReportSummaryResponse::productName)
                .containsExactly(expectedName(targets, 2), expectedName(targets, 1));
        assertPage(first, 3, true, 2);
        assertThat(first.getContent()).extracting(ReportSummaryResponse::reportId)
                .containsExactly(reportIds.get(2), reportIds.get(1));
        assertThat(first.getContent()).extracting(ReportSummaryResponse::reviewContentSummary)
                .containsExactly(expectedContent(targets, 2), expectedContent(targets, 1));
        Page<ReportSummaryResponse> next = reportService.getMyReports("owner@test.com", first.nextPageable());
        assertPage(next, 3, false, 1);
        assertThat(next.getContent()).extracting(ReportSummaryResponse::reportId).containsExactly(reportIds.get(0));
        assertThat(next.getContent()).extracting(ReportSummaryResponse::productName)
                .containsExactly(expectedName(targets, 0));
        assertThat(reportService.getMyReports("other@test.com", PageRequest.of(0, 2)).getTotalElements()).isEqualTo(1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"internal", "external", "mixed"})
    void myReviewFeedbackPagesRetainEveryTargetAndCount(String targets) {
        seed(targets);
        Page<FeedbackHistoryResponse> first = reviewService.getMyFeedbacks("owner@test.com", PageRequest.of(0, 2));
        assertThat(first.getContent()).extracting(FeedbackHistoryResponse::productName)
                .containsExactly(expectedName(targets, 2), expectedName(targets, 1));
        assertPage(first, 3, true, 2);
        assertThat(first.getContent()).extracting(FeedbackHistoryResponse::feedbackId)
                .containsExactly(feedbackIds.get(2), feedbackIds.get(1));
        assertThat(first.getContent()).extracting(FeedbackHistoryResponse::reviewContentSummary)
                .containsExactly(expectedContent(targets, 2), expectedContent(targets, 1));
        Page<FeedbackHistoryResponse> next = reviewService.getMyFeedbacks("owner@test.com", first.nextPageable());
        assertPage(next, 3, false, 1);
        assertThat(next.getContent()).extracting(FeedbackHistoryResponse::feedbackId).containsExactly(feedbackIds.get(0));
        assertThat(next.getContent()).extracting(FeedbackHistoryResponse::productName)
                .containsExactly(expectedName(targets, 0));
        assertThat(reviewService.getMyFeedbacks("other@test.com", PageRequest.of(0, 2)).getTotalElements()).isEqualTo(1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"internal", "external", "mixed"})
    void adminAllAndStatusPagesRetainEveryTarget(String targets) {
        seed(targets);
        Page<ReportResponse> all = adminService.getAllReports(null, PageRequest.of(0, 2));
        assertPage(all, 4, true, 2);
        assertThat(all.getContent()).extracting(ReportResponse::reportId)
                .containsExactly(reportIds.get(3), reportIds.get(2));
        assertThat(all.getContent()).extracting(ReportResponse::productName)
                .containsExactly("외부 상품", expectedName(targets, 2));
        Page<ReportResponse> next = adminService.getAllReports(null, all.nextPageable());
        assertPage(next, 4, false, 2);
        assertThat(next.getContent()).extracting(ReportResponse::reportId)
                .containsExactly(reportIds.get(1), reportIds.get(0));
        assertThat(next.getContent()).extracting(ReportResponse::productName)
                .containsExactly(expectedName(targets, 1), expectedName(targets, 0));
        Page<ReportResponse> pending = adminService.getAllReports(ReportStatus.PENDING, PageRequest.of(0, 1));
        assertPage(pending, 2, true, 1);
        assertThat(pending.getContent()).extracting(ReportResponse::reportId).containsExactly(reportIds.get(2));
        assertThat(pending.getContent()).extracting(ReportResponse::productName)
                .containsExactly(expectedName(targets, 2));
        Page<ReportResponse> pendingNext = adminService.getAllReports(ReportStatus.PENDING, pending.nextPageable());
        assertPage(pendingNext, 2, false, 1);
        assertThat(pendingNext.getContent()).extracting(ReportResponse::reportId).containsExactly(reportIds.get(0));
        assertThat(pendingNext.getContent()).extracting(ReportResponse::productName)
                .containsExactly(expectedName(targets, 0));
        assertThat(adminService.getAllReports(ReportStatus.REJECTED, PageRequest.of(0, 1))).isEmpty();
    }

    @Test
    void externalDetailsAreNullSafeAndRestrictedToOwner() {
        Report report = report(owner, null, "external-report", ReportStatus.PENDING, 0);
        ReviewFeedback feedback = feedback(owner, null, "external-feedback", 0);
        flushAndClear();
        ReportResponse detail = reportService.getMyReport(report.getId(), "owner@test.com");
        assertThat(detail.reviewId()).isNull();
        assertThat(detail.reviewContent()).isNull();
        assertThat(detail.productName()).isEqualTo("외부 상품");
        assertThat(detail.externalReviewId()).isEqualTo("external-report");
        assertThat(detail.productExternalId()).isEqualTo("kurly-p-2");
        FeedbackHistoryResponse history = reviewService.getMyFeedback(feedback.getId(), "owner@test.com");
        assertThat(history.reviewId()).isNull();
        assertThat(history.reviewContentSummary()).isNull();
        assertThat(history.productId()).isEqualTo(2L);
        assertThat(history.productName()).isEqualTo("외부 상품");
        assertThatThrownBy(() -> reportService.getMyReport(report.getId(), "other@test.com"))
                .isInstanceOfSatisfying(CustomException.class, ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.REPORT_NOT_FOUND));
        assertThatThrownBy(() -> reviewService.getMyFeedback(feedback.getId(), "other@test.com"))
                .isInstanceOfSatisfying(CustomException.class, ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.FEEDBACK_NOT_FOUND));
    }

    @Test
    void unifiedHistoryIncludesExternalReportsWithNullContent() {
        seed("mixed");
        List<UnifiedFeedbackResponse> result = unifiedService.getUnifiedFeedbacks("owner@test.com");
        assertThat(result).extracting(UnifiedFeedbackResponse::productName)
                .containsExactly("외부 상품", "내부 상품", "외부 상품");
        assertThat(result).extracting(UnifiedFeedbackResponse::feedbackCategory).containsOnly("REPORT");
        assertThat(result.get(0).reviewContent()).isNull();
        assertThat(result.get(1).reviewContent()).isEqualTo("내부 리뷰 1");
    }

    private void seed(String targets) {
        for (int i = 0; i < 3; i++) {
            boolean external = targets.equals("external") || targets.equals("mixed") && i != 1;
            Review review = external ? null : em.persist(Review.builder().product(internalProduct)
                    .content("내부 리뷰 " + i).reviewerId("reviewer-" + i).reviewerNickname("리뷰어")
                    .rtiScore(80.0).trustGrade(TrustGrade.SAFE).writtenAt(LocalDateTime.now()).build());
            report(owner, review, "report-" + i, i == 1 ? ReportStatus.ACCEPTED : ReportStatus.PENDING, i);
            feedback(owner, review, "feedback-" + i, i);
        }
        report(other, null, "other-report", ReportStatus.ACCEPTED, 3);
        feedback(other, null, "other-feedback", 3);
        flushAndClear();
    }

    private Report report(User user, Review review, String externalId, ReportStatus status, int order) {
        Report report = em.persist(Report.builder().reporter(user).review(review)
                .product(review == null ? externalProduct : null).externalReviewId(review == null ? externalId : null)
                .reason(ReportReason.AD_REVIEW).status(status).build());
        report.setCreatedAt(LocalDateTime.of(2026, 1, 1, 0, 0).plusMinutes(order));
        reportIds.add(report.getId());
        return report;
    }

    private ReviewFeedback feedback(User user, Review review, String externalId, int order) {
        ReviewFeedback feedback = em.persist(ReviewFeedback.builder().user(user).review(review)
                .product(review == null ? externalProduct : null).externalReviewId(review == null ? externalId : null)
                .feedbackType(FeedbackType.FAKE).build());
        feedback.setCreatedAt(LocalDateTime.of(2026, 1, 1, 0, 0).plusMinutes(order));
        feedbackIds.add(feedback.getId());
        return feedback;
    }

    private void flushAndClear() {
        em.flush();
        em.clear();
    }

    private String expectedName(String targets, int index) {
        return targets.equals("external") || targets.equals("mixed") && index != 1 ? "외부 상품" : "내부 상품";
    }

    private String expectedContent(String targets, int index) {
        return targets.equals("external") || targets.equals("mixed") && index != 1 ? null : "내부 리뷰 " + index;
    }

    private void assertPage(Page<?> page, long total, boolean hasNext, int size) {
        assertThat(page.getTotalElements()).isEqualTo(total);
        assertThat(page.hasNext()).isEqualTo(hasNext);
        assertThat(page.getContent()).hasSize(size);
    }
}

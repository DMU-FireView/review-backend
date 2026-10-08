package com.example.fireview.domain.feedback.service;

import com.example.fireview.domain.dataserver.DataServerProductKey;
import com.example.fireview.domain.dataserver.service.DataProductTagService;
import com.example.fireview.domain.feedback.dto.request.AnalysisFeedbackCreateRequest;
import com.example.fireview.domain.feedback.dto.response.AnalysisFeedbackResponse;
import com.example.fireview.domain.feedback.entity.AnalysisFeedback;
import com.example.fireview.domain.feedback.entity.AnalysisFeedbackType;
import com.example.fireview.domain.feedback.entity.UserJudgment;
import com.example.fireview.domain.feedback.repository.AnalysisFeedbackRepository;
import com.example.fireview.domain.notification.entity.NotificationType;
import com.example.fireview.domain.notification.service.NotificationService;
import com.example.fireview.domain.product.entity.Product;
import com.example.fireview.domain.review.entity.Review;
import com.example.fireview.domain.review.entity.TrustGrade;
import com.example.fireview.domain.review.repository.ReviewRepository;
import com.example.fireview.domain.user.entity.User;
import com.example.fireview.domain.user.service.UserService;
import com.example.fireview.global.exception.CustomException;
import com.example.fireview.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AnalysisFeedbackServiceTest {

    private static final String EMAIL = "u@test.com";
    private static final DataServerProductKey KEY = new DataServerProductKey("kurly", "1000146248");

    @Mock AnalysisFeedbackRepository feedbackRepository;
    @Mock ReviewRepository reviewRepository;
    @Mock DataProductTagService dataProductTagService;
    @Mock UserService userService;
    @Mock NotificationService notificationService;
    @InjectMocks AnalysisFeedbackService service;

    private final User user = User.builder().id(1L).email(EMAIL).nickname("tester").build();
    private final Product tag = Product.builder().id(42L).name("토리든 마스크팩").platform("KURLY")
            .dataPlatform("kurly").dataProductId("1000146248").build();

    private static AnalysisFeedbackCreateRequest request() {
        return new AnalysisFeedbackCreateRequest(AnalysisFeedbackType.SCORE_MISMATCH, UserJudgment.MORE_TRUSTWORTHY,
                List.of("repetition"), "점수가 너무 낮아요", null, null);
    }

    @BeforeEach
    void setUp() {
        when(userService.findByEmail(EMAIL)).thenReturn(user);
        when(feedbackRepository.save(any())).thenAnswer(inv -> {
            AnalysisFeedback f = inv.getArgument(0);
            f.setId(7L);
            f.setCreatedAt(LocalDateTime.now());
            return f;
        });
    }

    @Test
    void 외부_리뷰_피드백은_번호표와_외부_리뷰_ID로_저장되고_본문은_없다() {
        when(dataProductTagService.resolveOrCreate(KEY)).thenReturn(tag);

        AnalysisFeedbackResponse res = service.submitExternal("kurly", "1000146248", "r-99", EMAIL, request());

        ArgumentCaptor<AnalysisFeedback> saved = ArgumentCaptor.forClass(AnalysisFeedback.class);
        verify(feedbackRepository).save(saved.capture());
        assertThat(saved.getValue().getReview()).isNull();
        assertThat(saved.getValue().getProduct()).isSameAs(tag);
        assertThat(saved.getValue().getExternalReviewId()).isEqualTo("r-99");
        assertThat(saved.getValue().getSubmitter()).isSameAs(user);
        assertThat(saved.getValue().getRelatedSignals()).containsExactly("repetition");

        assertThat(res.feedbackId()).isEqualTo(7L);
        assertThat(res.reviewId()).isNull();
        assertThat(res.reviewContent()).isNull();
        assertThat(res.externalReviewId()).isEqualTo("r-99");
        assertThat(res.productExternalId()).isEqualTo("kurly-1000146248");
        assertThat(res.productName()).isEqualTo("토리든 마스크팩");

        verify(notificationService).createNotification(eq(user), eq(NotificationType.ANALYSIS_FEEDBACK_RECEIVED),
                anyString(), anyString(), eq("/feedback/me/7"));
        verifyNoInteractions(reviewRepository);
    }

    @Test
    void 수집_전_상품이면_번호표가_거절되고_저장하지_않는다() {
        when(dataProductTagService.resolveOrCreate(KEY))
                .thenThrow(new CustomException(ErrorCode.PRODUCT_NOT_COLLECTED));

        assertThatThrownBy(() -> service.submitExternal("kurly", "1000146248", "r-99", EMAIL, request()))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.PRODUCT_NOT_COLLECTED);

        verify(feedbackRepository, never()).save(any());
        verifyNoInteractions(notificationService);
    }

    @Test
    void Data_서버에_닿지_못하면_저장하지_않는다() {
        when(dataProductTagService.resolveOrCreate(KEY))
                .thenThrow(new CustomException(ErrorCode.DATA_SERVER_UNAVAILABLE));

        assertThatThrownBy(() -> service.submitExternal("kurly", "1000146248", "r-99", EMAIL, request()))
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.DATA_SERVER_UNAVAILABLE);
        verify(feedbackRepository, never()).save(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {" ", "\t"})
    void 외부_리뷰_ID가_비었으면_번호표_발급_전에_거절한다(String blank) {
        assertThatThrownBy(() -> service.submitExternal("kurly", "1000146248", blank, EMAIL, request()))
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INPUT);

        verifyNoInteractions(dataProductTagService);
        verify(feedbackRepository, never()).save(any());
    }

    @Test
    void 외부_리뷰_ID가_칼럼_길이를_넘으면_자르지_않고_거절한다() {
        String tooLong = "r".repeat(AnalysisFeedback.EXTERNAL_REVIEW_ID_MAX_LENGTH + 1);

        assertThatThrownBy(() -> service.submitExternal("kurly", "1000146248", tooLong, EMAIL, request()))
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INPUT);
        verifyNoInteractions(dataProductTagService);
    }

    @Test
    void 기존_Spring_리뷰_경로는_그대로_리뷰만_가리킨다() {
        Review review = Review.builder().id(5L).content("광고 같아요").rtiScore(30.0).trustGrade(TrustGrade.DANGER)
                .product(Product.builder().id(1L).name("기존 상품").build())
                .writtenAt(LocalDateTime.now()).reviewerNickname("익명").reviewerId("u1").build();
        when(reviewRepository.findById(5L)).thenReturn(Optional.of(review));

        AnalysisFeedbackResponse res = service.submit(5L, EMAIL, request());

        ArgumentCaptor<AnalysisFeedback> saved = ArgumentCaptor.forClass(AnalysisFeedback.class);
        verify(feedbackRepository).save(saved.capture());
        assertThat(saved.getValue().getReview()).isSameAs(review);
        assertThat(saved.getValue().getProduct()).isNull();
        assertThat(saved.getValue().getExternalReviewId()).isNull();

        assertThat(res.reviewId()).isEqualTo(5L);
        assertThat(res.reviewContent()).isEqualTo("광고 같아요");
        assertThat(res.productName()).isEqualTo("기존 상품");
        assertThat(res.externalReviewId()).isNull();
        assertThat(res.productExternalId()).isNull();
        verifyNoInteractions(dataProductTagService);
    }

    @Test
    void 없는_Spring_리뷰면_404() {
        when(reviewRepository.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.submit(5L, EMAIL, request()))
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.REVIEW_NOT_FOUND);
        verify(feedbackRepository, never()).save(any());
    }
}

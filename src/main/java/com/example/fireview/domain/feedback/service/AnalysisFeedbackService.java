package com.example.fireview.domain.feedback.service;

import com.example.fireview.domain.dataserver.DataServerProductKey;
import com.example.fireview.domain.dataserver.service.DataProductTagService;
import com.example.fireview.domain.feedback.dto.request.AnalysisFeedbackCreateRequest;
import com.example.fireview.domain.feedback.dto.response.AnalysisFeedbackResponse;
import com.example.fireview.domain.feedback.entity.AnalysisFeedback;
import com.example.fireview.domain.feedback.repository.AnalysisFeedbackRepository;
import com.example.fireview.domain.notification.entity.NotificationType;
import com.example.fireview.domain.notification.service.NotificationService;
import com.example.fireview.domain.product.entity.Product;
import com.example.fireview.domain.review.entity.Review;
import com.example.fireview.domain.review.repository.ReviewRepository;
import com.example.fireview.domain.user.entity.User;
import com.example.fireview.domain.user.service.UserService;
import com.example.fireview.global.exception.CustomException;
import com.example.fireview.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnalysisFeedbackService {

    private final AnalysisFeedbackRepository feedbackRepository;
    private final ReviewRepository reviewRepository;
    private final DataProductTagService dataProductTagService;
    private final UserService userService;
    private final NotificationService notificationService;

    /** 분석 피드백 제출 (Spring 리뷰) */
    @Transactional
    public AnalysisFeedbackResponse submit(Long reviewId, String email,
                                           AnalysisFeedbackCreateRequest request) {
        User user = userService.findByEmail(email);
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new CustomException(ErrorCode.REVIEW_NOT_FOUND));

        return save(user, AnalysisFeedback.builder().review(review), request);
    }

    /**
     * Data 서버 리뷰에 대한 분석 피드백 제출.
     *
     * <p>Spring DB 에 그 리뷰 행이 없으므로 상품 번호표와 리뷰 ID 만 붙잡는다. 번호표가 없으면 여기서
     * 발급되고, 아직 수집 전인 상품이면 발급 자체가 거절된다(신고·리뷰 피드백과 같은 수준).
     *
     * <p><b>검증하지 않는 것:</b> 그 리뷰가 실제로 그 상품에 있는지, 그 리뷰의 분석 결과가 있는지.
     * Data 서버에 리뷰 단건(또는 분석 결과 단건)을 확인하는 API 가 없고, 상품 분석 조회는 현재
     * 페이지의 결과만 돌려주어 "첫 페이지에 없다 = 없는 리뷰" 로 판단할 수 없다.
     * 번호표가 이미 있으면 Data 서버를 부르지도 않는다. 단건 검증 계약이 생기면 여기서 확인한다.
     */
    @Transactional
    public AnalysisFeedbackResponse submitExternal(String platform, String productId,
                                                   String externalReviewId, String email,
                                                   AnalysisFeedbackCreateRequest request) {
        // 칼럼 길이를 넘는 값을 조용히 자르지 않는다. 번호표 발급(외부 호출) 전에 거절한다
        if (externalReviewId == null || externalReviewId.isBlank()
                || externalReviewId.length() > AnalysisFeedback.EXTERNAL_REVIEW_ID_MAX_LENGTH) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
        User user = userService.findByEmail(email);
        Product product = dataProductTagService.resolveOrCreate(
                new DataServerProductKey(platform, productId));

        return save(user, AnalysisFeedback.builder().product(product).externalReviewId(externalReviewId),
                request);
    }

    /** 대상(리뷰 또는 번호표+외부 리뷰 ID)만 채운 빌더에 요청 내용을 붙여 저장하고 접수 알림을 보낸다 */
    private AnalysisFeedbackResponse save(User user, AnalysisFeedback.AnalysisFeedbackBuilder target,
                                          AnalysisFeedbackCreateRequest request) {
        AnalysisFeedback feedback = target
                .submitter(user)
                .feedbackType(request.feedbackType())
                .userJudgment(request.userJudgment())
                .detail(request.detail())
                .attachmentUrl(request.attachmentUrl())
                .replyEmail(request.replyEmail())
                .build();
        feedback.validateTarget();

        if (request.relatedSignals() != null) {
            feedback.getRelatedSignals().addAll(request.relatedSignals());
        }

        AnalysisFeedback saved = feedbackRepository.save(feedback);

        notificationService.createNotification(
                user,
                NotificationType.ANALYSIS_FEEDBACK_RECEIVED,
                "분석 피드백이 접수되었습니다",
                request.feedbackType().getDescription() + " 피드백이 접수되어 검토 중입니다.",
                "/feedback/me/" + saved.getId()
        );

        return AnalysisFeedbackResponse.from(saved);
    }

    /** 내 분석 피드백 목록 */
    public Page<AnalysisFeedbackResponse> getMyFeedbacks(String email, Pageable pageable) {
        User user = userService.findByEmail(email);
        return feedbackRepository.findBySubmitterIdWithReview(user.getId(), pageable)
                .map(AnalysisFeedbackResponse::from);
    }

    /** 내 분석 피드백 단건 */
    public AnalysisFeedbackResponse getMyFeedback(Long feedbackId, String email) {
        User user = userService.findByEmail(email);
        AnalysisFeedback feedback = feedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new CustomException(ErrorCode.FEEDBACK_NOT_FOUND));
        if (!feedback.getSubmitter().getId().equals(user.getId())) {
            throw new CustomException(ErrorCode.REPORT_FORBIDDEN);
        }
        return AnalysisFeedbackResponse.from(feedback);
    }
}

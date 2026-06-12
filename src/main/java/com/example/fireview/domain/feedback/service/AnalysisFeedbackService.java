package com.example.fireview.domain.feedback.service;

import com.example.fireview.domain.feedback.dto.request.AnalysisFeedbackCreateRequest;
import com.example.fireview.domain.feedback.dto.response.AnalysisFeedbackResponse;
import com.example.fireview.domain.feedback.entity.AnalysisFeedback;
import com.example.fireview.domain.feedback.repository.AnalysisFeedbackRepository;
import com.example.fireview.domain.notification.entity.NotificationType;
import com.example.fireview.domain.notification.service.NotificationService;
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
    private final UserService userService;
    private final NotificationService notificationService;

    /** 분석 피드백 제출 */
    @Transactional
    public AnalysisFeedbackResponse submit(Long reviewId, String email,
                                           AnalysisFeedbackCreateRequest request) {
        User user = userService.findByEmail(email);
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new CustomException(ErrorCode.REVIEW_NOT_FOUND));

        AnalysisFeedback feedback = AnalysisFeedback.builder()
                .submitter(user)
                .review(review)
                .feedbackType(request.feedbackType())
                .userJudgment(request.userJudgment())
                .detail(request.detail())
                .attachmentUrl(request.attachmentUrl())
                .replyEmail(request.replyEmail())
                .build();

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

package com.example.fireview.domain.review.service;

import com.example.fireview.domain.product.entity.Product;
import com.example.fireview.domain.product.repository.ProductRepository;
import com.example.fireview.domain.review.dto.FeedbackHistoryResponse;
import com.example.fireview.domain.review.dto.ReviewFeedbackRequest;
import com.example.fireview.domain.review.dto.ReviewResponse;
import com.example.fireview.domain.review.entity.Review;
import com.example.fireview.domain.review.entity.ReviewFeedback;
import com.example.fireview.domain.review.repository.ReviewFeedbackRepository;
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

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ReviewFeedbackRepository feedbackRepository;
    private final ProductRepository productRepository;
    private final UserService userService;
    private final AtiService atiService;

    public List<ReviewResponse> getReviewsByProduct(Long productId, Double minScore) {
        // naverProductId로 요청 시 실제 DB id로 변환 (네이버 상품 ID → DB PK)
        Long resolvedId = productRepository.findById(productId)
                .map(Product::getId)
                .orElseGet(() -> productRepository.findByNaverProductId(String.valueOf(productId))
                        .map(Product::getId)
                        .orElse(productId));

        java.util.Map<String, Double> atiMap = atiService.calculateAtiMapByProduct(resolvedId);

        if (minScore != null && minScore > 0) {
            return reviewRepository
                    .findByProduct_IdAndRtiScoreGreaterThanEqual(resolvedId, minScore)
                    .stream()
                    .map(r -> ReviewResponse.of(r, atiMap.getOrDefault(r.getReviewerId(), 50.0)))
                    .toList();
        }
        return reviewRepository.findByProduct_IdOrderByRtiScoreDesc(resolvedId)
                .stream()
                .map(r -> ReviewResponse.of(r, atiMap.getOrDefault(r.getReviewerId(), 50.0)))
                .toList();
    }

    @Transactional
    public void submitFeedback(Long reviewId, String userEmail, ReviewFeedbackRequest request) {
        User user = userService.findByEmail(userEmail);
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new CustomException(ErrorCode.REVIEW_NOT_FOUND));

        if (feedbackRepository.existsByReview_IdAndUser_Id(reviewId, user.getId())) {
            throw new CustomException(ErrorCode.FEEDBACK_ALREADY_EXISTS);
        }

        ReviewFeedback feedback = ReviewFeedback.builder()
                .review(review)
                .user(user)
                .feedbackType(request.feedbackType())
                .build();
        feedbackRepository.save(feedback);
    }

    /** 내가 제출한 피드백 목록 조회 */
    public Page<FeedbackHistoryResponse> getMyFeedbacks(String userEmail, Pageable pageable) {
        User user = userService.findByEmail(userEmail);
        return feedbackRepository.findByUserIdWithReview(user.getId(), pageable)
                .map(FeedbackHistoryResponse::from);
    }

    /** 내가 제출한 피드백 단건 조회 */
    public FeedbackHistoryResponse getMyFeedback(Long feedbackId, String userEmail) {
        User user = userService.findByEmail(userEmail);
        return feedbackRepository.findByIdAndUser_Id(feedbackId, user.getId())
                .map(FeedbackHistoryResponse::from)
                .orElseThrow(() -> new CustomException(ErrorCode.FEEDBACK_NOT_FOUND));
    }

    @Transactional
    public Review save(Review review) {
        Review saved = reviewRepository.save(review);
        recalculateProductRti(saved.getProduct());
        return saved;
    }

    private void recalculateProductRti(Product product) {
        Double avg = reviewRepository.findAvgRtiByProductId(product.getId());
        if (avg != null) {
            product.setAvgRti(Math.round(avg * 10.0) / 10.0);
            productRepository.save(product);
        }
    }
}
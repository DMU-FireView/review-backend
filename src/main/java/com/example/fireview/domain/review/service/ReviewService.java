package com.example.fireview.domain.review.service;

import com.example.fireview.domain.product.entity.Product;
import com.example.fireview.domain.product.repository.ProductRepository;
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
        // 상품 내 모든 리뷰어의 ATI를 한 번에 조회 (N+1 방지)
        java.util.Map<String, Double> atiMap = atiService.calculateAtiMapByProduct(productId);

        if (minScore != null && minScore > 0) {
            return reviewRepository
                    .findByProduct_IdAndRtiScoreGreaterThanEqual(productId, minScore)
                    .stream()
                    .map(r -> ReviewResponse.of(r, atiMap.getOrDefault(r.getReviewerId(), 50.0)))
                    .toList();
        }
        return reviewRepository.findByProduct_IdOrderByRtiScoreDesc(productId)
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
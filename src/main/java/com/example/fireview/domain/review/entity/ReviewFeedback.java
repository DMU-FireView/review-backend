package com.example.fireview.domain.review.entity;

import com.example.fireview.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "review_feedbacks",
        uniqueConstraints = @UniqueConstraint(columnNames = {"review_id", "user_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewFeedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 피드백 대상 리뷰. <b>Data 서버 리뷰면 null 이다.</b> */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id")
    private Review review;

    /** Data 서버 리뷰일 때의 상품 번호표. {@link #review} 가 null 이면 채워진다 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private com.example.fireview.domain.product.entity.Product product;

    /** Data 서버 리뷰 ID. {@link #review} 가 null 이면 채워진다 */
    @Column(name = "external_review_id", length = 200)
    private String externalReviewId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FeedbackType feedbackType;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    /** Data 서버 리뷰에 대한 피드백인지 */
    public boolean isExternalReview() {
        return review == null;
    }

    /** Spring 리뷰 ID. Data 서버 리뷰면 null */
    public Long reviewIdOrNull() {
        return review == null ? null : review.getId();
    }

    /**
     * 피드백 대상 리뷰 본문. <b>Data 서버 리뷰면 null</b> — 본문을 복사해 두지 않는다.
     * 운영자는 상품으로 들어가 해당 리뷰를 확인한다.
     */
    public String reviewContentOrNull() {
        return review == null ? null : review.getContent();
    }

    /** 상품 ID. 어느 쪽 경로든 채워진다 */
    public Long productIdOrNull() {
        if (review != null && review.getProduct() != null) {
            return review.getProduct().getId();
        }
        return product == null ? null : product.getId();
    }

    /** 상품명. 어느 쪽 경로든 채워진다 */
    public String productNameOrNull() {
        if (review != null && review.getProduct() != null) {
            return review.getProduct().getName();
        }
        return product == null ? null : product.getName();
    }

    /** Data 서버 상품 주소. Spring 리뷰면 null */
    public String productExternalIdOrNull() {
        return product == null ? null : product.dataServerExternalId();
    }
}

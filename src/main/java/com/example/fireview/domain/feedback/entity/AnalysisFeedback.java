package com.example.fireview.domain.feedback.entity;

import com.example.fireview.domain.product.entity.Product;
import com.example.fireview.domain.review.entity.Review;
import com.example.fireview.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * AI 분석 결과에 대한 사용자 피드백
 *
 * 리뷰의 RTI 점수나 분석 설명에 이의가 있을 때 제출하는 상세 피드백.
 * 단순 REAL/FAKE 투표인 ReviewFeedback과 별개 엔티티.
 *
 * <p>대상 리뷰는 둘 중 정확히 하나로 가리킨다. 저장·수정 직전에 {@link #validateTarget()} 이 확인한다.
 * <ul>
 *   <li>Spring 리뷰: {@link #review}</li>
 *   <li>Data 서버 리뷰: {@link #product}(상품 번호표) + {@link #externalReviewId}</li>
 * </ul>
 */
@Entity
@Table(name = "analysis_feedbacks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnalysisFeedback {

    /** {@code external_review_id} 칼럼 길이. 넘는 값은 자르지 않고 거절한다 */
    public static final int EXTERNAL_REVIEW_ID_MAX_LENGTH = 200;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User submitter;

    /**
     * 대상 Spring 리뷰. <b>Data 서버 리뷰에 대한 피드백이면 null 이다.</b>
     * Spring DB 에 행이 없는 리뷰라 FK 로 가리킬 수 없다.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id")
    private Review review;

    /**
     * Data 서버 리뷰에 대한 피드백일 때의 상품 번호표. {@link #review} 가 null 이면 채워진다.
     *
     * <p>리뷰 본문을 복사해 두지 않고 상품만 붙잡는다. 본문을 클라이언트에게 받으면 위조할 수 있고,
     * Data 서버에서 찾아오려면 리뷰가 몇 번째 페이지에 있는지 모른다. 운영자는 이 상품으로 들어가 확인한다.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    /** Data 서버 리뷰 ID (쇼핑몰이 발급한 원본 값). {@link #review} 가 null 이면 채워진다 */
    @Column(name = "external_review_id", length = EXTERNAL_REVIEW_ID_MAX_LENGTH)
    private String externalReviewId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AnalysisFeedbackType feedbackType;

    @Enumerated(EnumType.STRING)
    private UserJudgment userJudgment;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "analysis_feedback_signals",
            joinColumns = @JoinColumn(name = "feedback_id"))
    @Column(name = "signal")
    @Builder.Default
    private List<String> relatedSignals = new ArrayList<>();

    @Column(length = 2000)
    private String detail;

    @Column(length = 1000)
    private String attachmentUrl;

    @Column(length = 200)
    private String replyEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private AnalysisFeedbackStatus status = AnalysisFeedbackStatus.SUBMITTED;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        validateTarget();
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        validateTarget();
        updatedAt = LocalDateTime.now();
    }

    /**
     * 대상이 Spring 리뷰와 Data 서버 리뷰 중 정확히 하나인지 확인한다.
     *
     * <p>둘 다 채워지면 어느 리뷰에 대한 피드백인지 모호하고, 둘 다 비면 운영자가 찾아갈 곳이 없다.
     * DB 에는 아직 CHECK 를 걸지 않았다(기존 행 감사 전). 그래서 여기서 막는다.
     *
     * @throws IllegalStateException 둘 다 채워졌거나 둘 다 비었을 때, 외부 경로의 한쪽만 채워졌을 때
     */
    public void validateTarget() {
        boolean internal = review != null;
        boolean hasProduct = product != null;
        boolean hasExternalId = externalReviewId != null && !externalReviewId.isBlank();
        if (internal && (hasProduct || externalReviewId != null)) {
            throw new IllegalStateException("분석 피드백 대상은 Spring 리뷰와 Data 서버 리뷰 중 하나만 가리켜야 한다");
        }
        if (!internal && !(hasProduct && hasExternalId)) {
            throw new IllegalStateException("분석 피드백 대상이 없다: Spring 리뷰 또는 (상품 번호표 + 외부 리뷰 ID)가 필요하다");
        }
        if (hasExternalId && externalReviewId.length() > EXTERNAL_REVIEW_ID_MAX_LENGTH) {
            throw new IllegalStateException("외부 리뷰 ID 가 " + EXTERNAL_REVIEW_ID_MAX_LENGTH + "자를 넘는다");
        }
    }

    /** Data 서버 리뷰에 대한 피드백인지 */
    public boolean isExternalReview() {
        return review == null;
    }

    /** Spring 리뷰 ID. Data 서버 리뷰면 null */
    public Long reviewIdOrNull() {
        return review == null ? null : review.getId();
    }

    /** 대상 리뷰 본문. <b>Data 서버 리뷰면 null</b> — 본문을 복사해 두지 않는다 */
    public String reviewContentOrNull() {
        return review == null ? null : review.getContent();
    }

    /** 상품명. 어느 쪽 경로든 채워진다 */
    public String productNameOrNull() {
        if (review != null && review.getProduct() != null) {
            return review.getProduct().getName();
        }
        return product == null ? null : product.getName();
    }

    /** Data 서버 상품 주소({@code platform-productId}). Spring 리뷰면 null */
    public String productExternalIdOrNull() {
        return product == null ? null : product.dataServerExternalId();
    }
}

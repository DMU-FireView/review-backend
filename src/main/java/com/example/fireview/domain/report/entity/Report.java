package com.example.fireview.domain.report.entity;

import com.example.fireview.domain.product.entity.Product;
import com.example.fireview.domain.review.entity.Review;
import com.example.fireview.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 리뷰 신고 엔티티
 *
 * - reporter: 신고한 사용자
 * - review: 신고 대상 리뷰
 * - reason: 신고 사유 (enum)
 * - detail: 기타 사유 상세 설명 (optional)
 * - status: 신고 처리 상태 (PENDING → UNDER_REVIEW → ACCEPTED/REJECTED)
 * - adminComment: 관리자 처리 코멘트
 */
@Entity
@Table(
    name = "reports",
    uniqueConstraints = @UniqueConstraint(columnNames = {"reporter_id", "review_id"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id", nullable = false)
    private User reporter;

    /**
     * 신고 대상 리뷰. <b>Data 서버 리뷰를 신고하면 null 이다.</b>
     * Spring DB 에 행이 없는 리뷰라 FK 로 가리킬 수 없다.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id")
    private Review review;

    /**
     * Data 서버 리뷰를 신고할 때 쓰는 상품 번호표. {@link #review} 가 null 이면 채워진다.
     *
     * <p>리뷰 본문을 복사해 두지 않고 상품만 붙잡는 이유: 본문을 클라이언트에게 받으면
     * 신고 내용을 위조할 수 있고, Data 서버에서 찾아오려면 리뷰가 몇 번째 페이지에
     * 있는지 모른다. 운영자는 이 상품으로 들어가 해당 리뷰를 직접 확인한다.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    /** Data 서버 리뷰 ID (쇼핑몰이 발급한 원본 값). {@link #review} 가 null 이면 채워진다 */
    @Column(name = "external_review_id", length = 200)
    private String externalReviewId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportReason reason;

    @Column(length = 500)
    private String detail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ReportStatus status = ReportStatus.PENDING;

    @Column(length = 500)
    private String adminComment;

    /** 첨부 자료 URL (스크린샷 등, optional) */
    @Column(length = 1000)
    private String attachmentUrl;

    /** AI 분석 근거 함께 제출 여부 */
    @Builder.Default
    @Column(nullable = false)
    private boolean includeAiEvidence = false;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /** Data 서버 리뷰에 대한 신고인지 */
    public boolean isExternalReview() {
        return review == null;
    }

    /** Spring 리뷰 ID. Data 서버 리뷰면 null */
    public Long reviewIdOrNull() {
        return review == null ? null : review.getId();
    }

    /**
     * 신고된 리뷰 본문. <b>Data 서버 리뷰면 null</b> — 본문을 복사해 두지 않는다.
     * 운영자는 상품으로 들어가 해당 리뷰를 확인한다.
     */
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

    /** Data 서버 상품 주소. Spring 리뷰면 null */
    public String productExternalIdOrNull() {
        return product == null ? null : product.dataServerExternalId();
    }
}

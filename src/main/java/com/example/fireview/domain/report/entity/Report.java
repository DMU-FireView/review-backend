package com.example.fireview.domain.report.entity;

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
    uniqueConstraints = @UniqueConstraint(columnNames = {"reviewer_id", "review_id"})
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
    @JoinColumn(name = "reviewer_id", nullable = false)
    private User reporter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id", nullable = false)
    private Review review;

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
}

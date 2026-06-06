package com.example.fireview.domain.feedback.entity;

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
 */
@Entity
@Table(name = "analysis_feedbacks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnalysisFeedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User submitter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id", nullable = false)
    private Review review;

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
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

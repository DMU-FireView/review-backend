package com.example.fireview.domain.user.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    /** 신고 처리 결과 알림 */
    @Builder.Default
    @Column(nullable = false)
    private boolean notifyReportResult = true;

    /** 피드백 관련 알림 */
    @Builder.Default
    @Column(nullable = false)
    private boolean notifyFeedback = true;

    /** 마케팅/이벤트 알림 */
    @Builder.Default
    @Column(nullable = false)
    private boolean notifyMarketing = false;
}

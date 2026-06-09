package com.example.fireview.domain.feedback.service;

import com.example.fireview.domain.feedback.dto.response.UnifiedFeedbackResponse;
import com.example.fireview.domain.feedback.repository.AnalysisFeedbackRepository;
import com.example.fireview.domain.report.repository.ReportRepository;
import com.example.fireview.domain.user.entity.User;
import com.example.fireview.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FeedbackStatusService {

    private final ReportRepository reportRepository;
    private final AnalysisFeedbackRepository analysisFeedbackRepository;
    private final UserService userService;

    /**
     * 내 신고 + 분석 피드백 통합 목록 (최신순, 최대 50건)
     */
    public List<UnifiedFeedbackResponse> getUnifiedFeedbacks(String email) {
        User user = userService.findByEmail(email);

        List<UnifiedFeedbackResponse> result = new ArrayList<>();

        reportRepository.findByReporterIdWithReview(user.getId(), PageRequest.of(0, 25))
                .forEach(r -> result.add(UnifiedFeedbackResponse.fromReport(r)));

        analysisFeedbackRepository.findBySubmitterIdWithReview(user.getId(), PageRequest.of(0, 25))
                .forEach(f -> result.add(UnifiedFeedbackResponse.fromAnalysisFeedback(f)));

        result.sort(Comparator.comparing(UnifiedFeedbackResponse::createdAt).reversed());
        return result;
    }
}

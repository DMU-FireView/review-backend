package com.example.fireview.domain.admin.dto.response;

public record AdminDashboardResponse(
        long totalReviews,
        long pendingReports,
        long pendingAnalysisFeedbacks,
        long totalUsers,
        long suspiciousReviewCount,   // RTI 50 미만
        long dangerReviewCount        // TrustGrade DANGER
) {}

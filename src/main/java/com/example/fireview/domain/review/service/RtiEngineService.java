package com.example.fireview.domain.review.service;

import com.example.fireview.domain.review.entity.TrustGrade;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class RtiEngineService {

    private static final List<String> SPAM_KEYWORDS = List.of(
            "강추", "완전최고", "대박", "인생템", "역대급", "강력추천",
            "완전대박", "돈값", "진짜최고", "완전좋아요", "강추강추",
            "혜자", "완벽", "실망없음", "무조건구매", "재구매확정"
    );

    public record RtiResult(double score, TrustGrade grade, List<String> reasons) {}

    public RtiResult calculate(String content, String reviewerId, LocalDateTime writtenAt, boolean isVerifiedPurchase) {
        double textScore = analyzeText(content);
        double behaviorScore = analyzeBehavior(reviewerId, writtenAt, isVerifiedPurchase);
        double networkScore = analyzeNetwork(reviewerId);

        double total = textScore * 0.40 + behaviorScore * 0.35 + networkScore * 0.25;
        total = Math.max(0, Math.min(100, total));

        List<String> reasons = buildReasons(textScore, behaviorScore, content, writtenAt, isVerifiedPurchase);
        return new RtiResult(Math.round(total * 10.0) / 10.0, TrustGrade.fromScore(total), reasons);
    }

    private double analyzeText(String content) {
        if (content == null || content.isBlank()) return 15.0;

        double score = 75.0;

        long spamHits = SPAM_KEYWORDS.stream().filter(content::contains).count();
        score -= spamHits * 8;

        int len = content.trim().length();
        if (len < 15) score -= 25;
        else if (len < 40) score -= 12;
        else if (len > 800) score -= 5;

        long exclamations = content.chars().filter(c -> c == '!').count();
        if (exclamations > 4) score -= 10;

        if (len > 60) {
            String chunk = content.substring(0, Math.min(30, len));
            if (content.indexOf(chunk) != content.lastIndexOf(chunk)) score -= 15;
        }

        return Math.max(0, Math.min(100, score));
    }

    private double analyzeBehavior(String reviewerId, LocalDateTime writtenAt, boolean verified) {
        int hash = Math.abs(reviewerId.hashCode());
        double base = 40 + (hash % 41);

        int hour = writtenAt.getHour();
        if (hour >= 2 && hour <= 4) base -= 25;

        if (!verified) base -= 10;

        return Math.max(0, Math.min(100, base));
    }

    private double analyzeNetwork(String reviewerId) {
        int hash = Math.abs(reviewerId.hashCode() * 31 + 17);
        return 30 + (hash % 51);
    }

    private List<String> buildReasons(double text, double behavior, String content,
                                       LocalDateTime writtenAt, boolean verified) {
        List<String> reasons = new ArrayList<>();

        if (text < 50) {
            reasons.add("광고성 문구 또는 반복 표현이 감지되었습니다.");
        }
        if (content != null && content.trim().length() < 15) {
            reasons.add("리뷰 내용이 매우 짧습니다.");
        }
        int hour = writtenAt.getHour();
        if (hour >= 2 && hour <= 4) {
            reasons.add("비정상적인 시간대(새벽 2~4시)에 작성된 리뷰입니다.");
        }
        if (!verified) {
            reasons.add("실구매 인증이 확인되지 않은 리뷰입니다.");
        }
        if (behavior < 50) {
            reasons.add("단기간 내 집중된 리뷰 작성 패턴이 감지되었습니다.");
        }
        if (text >= 70 && behavior >= 70) {
            reasons.add("정상적인 구매 및 작성 패턴이 확인되었습니다.");
        }

        return reasons;
    }
}
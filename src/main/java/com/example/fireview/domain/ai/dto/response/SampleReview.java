package com.example.fireview.domain.ai.dto.response;

import java.util.List;

/**
 * 대표 이상 리뷰 표출용 샘플 (명세서 v11.0 §3.5 SampleReview)
 *
 * - author: 작성자 마스킹 ID
 * - date: 실제 리뷰 작성 일자 (YYYY.MM.DD)
 * - level: 위험 수준 라벨 (위험/의심/경고 등)
 * - reasons: 프론트엔드 태그 노출용 사유 배열 (기존 tags 대체)
 */
public record SampleReview(
        String review_id,
        String author,
        String date,
        Integer rating,
        String content,
        String level,
        List<AiReason> reasons
) {}

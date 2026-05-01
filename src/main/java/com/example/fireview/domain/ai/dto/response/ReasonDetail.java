package com.example.fireview.domain.ai.dto.response;

/**
 * 상세 사유 카드 객체 (명세서 v11.0 §4.4 ReasonDetail)
 *
 * API 4 (리포트 상세) 에서 카드로 렌더링될 때 사용된다.
 * (percentage 속성은 명세서 v11.0 에서 삭제됨)
 */
public record ReasonDetail(
        String title,
        String description
) {}

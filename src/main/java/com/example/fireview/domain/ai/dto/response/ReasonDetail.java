package com.example.fireview.domain.ai.dto.response;

/**
 * 레거시: 새 구조에서는 Data 서버가 ai.re-view.kr 를 호출. 이 경로는 프론트 옛 상세 화면 호환용.
 * 상세 사유 카드 객체 (명세서 v11.0 §4.4 ReasonDetail)
 *
 * API 4 (리포트 상세) 에서 카드로 렌더링될 때 사용된다.
 * (percentage 속성은 명세서 v11.0 에서 삭제됨)
 */
public record ReasonDetail(
        String title,
        String description
) {}

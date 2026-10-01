package com.example.fireview.domain.chat.entity;

/**
 * 챗봇 엔드포인트 등급.
 *
 * <p>요금제({@code PlanTier})와 1:1 이 아니다. 요금제는 세 가지지만
 * 엔드포인트는 두 개로 나눈다. FREE·PLUS 는 같은 품질의 응답을 쓰고
 * 하루 사용량만 다르며, PRO 만 별도 경로에서 고급 모델을 쓴다.
 *
 * <ul>
 *   <li>{@link #STANDARD} — {@code POST /api/chat/messages}</li>
 *   <li>{@link #PRO} — {@code POST /api/chat/pro/messages}</li>
 * </ul>
 *
 * <p>경로를 쿼리 파라미터나 요청 본문 필드로 구분하지 않고 따로 뗀 이유:
 * 과금 경계를 URL 로 드러내면 접근 통제와 호출량 집계를 경로 단위로 할 수 있고,
 * 프론트에서도 어느 요금제용 호출인지 코드만 보고 안다.
 */
public enum ChatTier {

    /** 기본 응답. 모든 요금제가 쓸 수 있다 */
    STANDARD,

    /** 고급 응답(더 긴 답변·상위 모델). PRO 요금제만 쓸 수 있다 */
    PRO
}

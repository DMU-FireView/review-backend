package com.example.fireview.domain.user.entity;

/**
 * 챗봇 요금제 등급.
 *
 * <p>{@link Role} 과는 역할이 다르다. Role 은 "관리자냐 일반 사용자냐"(권한)이고
 * PlanTier 는 "얼마나 쓸 수 있냐"(과금)다. 둘을 한 enum 에 섞으면
 * 관리자이면서 무료 요금제인 조합을 표현할 수 없다.
 *
 * <p>요금제는 JWT 클레임이 아니라 DB 값이다. 결제·만료로 수시로 바뀌는 값이라
 * 토큰에 넣으면 재로그인 전까지 반영되지 않는다. 그래서 등급 검사는
 * SecurityConfig 가 아니라 서비스 계층({@code ChatPlanPolicy})에서 한다.
 */
public enum PlanTier {

    /** 무료. 하루 메시지 수가 제한되고 PRO 엔드포인트는 쓸 수 없다 */
    FREE("무료"),

    /** 유료 1단계 */
    PLUS("플러스"),

    /** 유료 2단계. PRO 엔드포인트(고급 모델) 사용 가능 */
    PRO("프로");

    private final String displayName;

    PlanTier(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /** 유료 요금제인지 */
    public boolean isPaid() {
        return this != FREE;
    }
}

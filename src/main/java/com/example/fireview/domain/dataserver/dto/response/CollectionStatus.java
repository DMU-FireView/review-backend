package com.example.fireview.domain.dataserver.dto.response;

/**
 * 수집 신선도. Data 서버의 {@code status} 를 그대로 올린다.
 *
 * <p>프론트가 이 값으로 화면을 가른다. 숨기면 사용자가 빈 화면을 보고 "고장났다"고 느낀다.
 */
public enum CollectionStatus {

    /** 최신. 그대로 보여주면 된다 */
    FRESH,

    /**
     * 조금 오래됐지만 쓸 수 있다. 수집은 뒤에서 돌고 있다.
     *
     * <p>실패가 아니다. 그대로 보여주고 필요하면 "갱신 중" 표시만 덧붙인다.
     * 최신을 기다리면 화면이 크롤링 속도에 묶인다.
     */
    STALE,

    /**
     * 아직 수집 전이라 보여줄 게 없다. {@code job} 으로 진행 상황을 볼 수 있다.
     *
     * <p>처음 보는 상품은 반드시 여기를 거친다. 로딩 화면이 필요하다.
     */
    QUEUED,

    /** Data 서버에 닿지 못했다 (설정 누락·토큰 오류·타임아웃) */
    UNAVAILABLE;

    public static CollectionStatus from(String raw) {
        if (raw == null) return UNAVAILABLE;
        return switch (raw) {
            case "fresh" -> FRESH;
            case "stale" -> STALE;
            case "queued" -> QUEUED;
            default -> UNAVAILABLE;
        };
    }
}

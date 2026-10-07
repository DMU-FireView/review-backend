package com.example.fireview.domain.dataserver.dto.response;

/**
 * 신뢰도 분석 상태. Data 서버의 {@code analysis.status} 를 그대로 올린다.
 *
 * <p>수집 신선도({@link CollectionStatus})와 별개다. 상품은 최신인데 분석은 아직일 수 있다.
 * 리뷰별 {@code rti}·{@code level} 은 {@link #DONE} 일 때만 채워진다.
 */
public enum AnalysisStatus {

    /** Data 서버에서 분석 기능이 꺼져 있다. 기다려도 결과가 오지 않는다 */
    DISABLED,

    /** 분석 job 이 아직 없다 */
    NOT_ANALYZED,

    /** 분석 대기 중 */
    QUEUED,

    /** 분석 중 */
    RUNNING,

    /** 분석 완료. 리뷰별 결과가 붙는다 */
    DONE,

    /** 분석 실패 */
    FAILED,

    /**
     * 마지막 분석 뒤 리뷰 구성이나 모델·정책이 바뀌었다. 새 분석 전까지 결과가 오지 않는다.
     *
     * <p>수집의 {@code STALE} 과 이름은 같지만 뜻이 다르다. 이쪽은 보여줄 결과가 없다.
     */
    STALE,

    /**
     * 분석 상태를 알 수 없다. Data 서버에 닿지 못했거나, {@code analysis} 를 보내지 않는
     * 구버전 Data 서버이거나, 처음 보는 상태값이다.
     */
    UNAVAILABLE;

    public static AnalysisStatus from(String raw) {
        if (raw == null) return UNAVAILABLE;
        return switch (raw) {
            case "disabled" -> DISABLED;
            case "not_analyzed" -> NOT_ANALYZED;
            case "queued" -> QUEUED;
            case "running" -> RUNNING;
            case "done" -> DONE;
            case "failed" -> FAILED;
            case "stale" -> STALE;
            default -> UNAVAILABLE;
        };
    }
}

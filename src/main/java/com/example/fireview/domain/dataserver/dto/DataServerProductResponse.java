package com.example.fireview.domain.dataserver.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * 상품 조회 응답 전체.
 *
 * <p>Data 서버는 요청이 크롤링을 기다리지 않는다. 신선도에 따라 셋 중 하나로 답한다.
 *
 * <table>
 *   <tr><th>status</th><th>HTTP</th><th>의미</th></tr>
 *   <tr><td>{@code fresh}</td><td>200</td><td>TTL 이내. 그대로 쓰면 된다</td></tr>
 *   <tr><td>{@code stale}</td><td>200</td><td>TTL 지남. 마지막 정상 데이터가 함께 온다. 수집은 뒤에서 돈다</td></tr>
 *   <tr><td>{@code queued}</td><td>202</td><td>데이터가 아예 없다. {@code product} 가 비어 있고 job 만 있다</td></tr>
 * </table>
 *
 * <p>{@code stale} 을 실패로 다루면 안 된다. 쓸 수 있는 데이터가 들어 있고,
 * 매번 최신을 기다리면 화면이 크롤링 속도에 묶인다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record DataServerProductResponse(
        String status,
        DataServerProduct product,
        Reviews reviews,
        DataServerJob job
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Reviews(
            List<DataServerReview> items,
            @JsonProperty("next_cursor") String nextCursor
    ) {}

    /** 지금 바로 쓸 수 있는 데이터가 있는지 (fresh 또는 stale) */
    public boolean hasUsableData() {
        return product != null && !"queued".equals(status);
    }

    /** 아직 수집 전이라 기다려야 하는지 */
    public boolean isQueued() {
        return "queued".equals(status);
    }

    public List<DataServerReview> reviewItems() {
        return reviews == null || reviews.items() == null ? List.of() : reviews.items();
    }
}

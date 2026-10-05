package com.example.fireview.domain.dataserver.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 수집 job 상태.
 *
 * <p>{@code status} 는 pending / running / succeeded / partial / failed.
 * 상품만 성공하고 리뷰가 실패한 부분 실패를 표현하려고 하위 상태를 따로 둔다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record DataServerJob(
        Long id,
        String platform,
        @JsonProperty("product_id") String productId,
        String status,
        @JsonProperty("product_status") String productStatus,
        @JsonProperty("review_status") String reviewStatus,
        @JsonProperty("last_error") String lastError
) {
    public boolean isFinished() {
        return "succeeded".equals(status) || "partial".equals(status) || "failed".equals(status);
    }
}

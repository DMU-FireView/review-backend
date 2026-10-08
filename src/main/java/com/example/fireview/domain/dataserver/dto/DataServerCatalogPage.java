package com.example.fireview.domain.dataserver.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/** 저장 상품과 최신 분석의 목록용 요약. 리뷰 원본은 포함하지 않는다. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record DataServerCatalogPage(
        List<Entry> items,
        @JsonProperty("next_cursor") String nextCursor) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Entry(DataServerProduct product, Analysis analysis) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Analysis(
            String status,
            @JsonProperty("job_id") Long jobId,
            @JsonProperty("avg_rti") Double avgRti,
            @JsonProperty("scored_review_count") Integer scoredReviewCount,
            @JsonProperty("review_count") Integer reviewCount,
            @JsonProperty("source_review_count") Integer sourceReviewCount,
            Boolean sampled,
            @JsonProperty("model_version") String modelVersion,
            @JsonProperty("policy_version") String policyVersion) {}
}

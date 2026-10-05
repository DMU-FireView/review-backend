package com.example.fireview.domain.dataserver.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Data 서버 리뷰 한 건.
 *
 * <p>평점이 {@code Double} 인 점에 주의한다. Data 서버는 {@code NUMERIC(3,2)} 로 저장해
 * 4.5 같은 값이 올 수 있다. Spring 쪽 {@code Integer} 평점으로 옮길 때는 반올림한다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record DataServerReview(
        @JsonProperty("review_id") String reviewId,
        String content,
        Double rating,
        String author,
        @JsonProperty("written_at") String writtenAt,
        String option,
        List<String> images,
        @JsonProperty("helpful_count") Integer helpfulCount
) {}

package com.example.fireview.domain.ai.dto.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * AI 서버에 전달하는 분석 트리거 요청 DTO (TriggerRequest)
 *
 * AI 서버 API 연동 명세서 v11.0 준수.
 * 백엔드는 무거운 리뷰 데이터를 전송하지 않고 식별자(product_id) 만 전송하여
 * AI 서버의 연산을 트리거한다. AI 서버가 product_id 기반으로 자체 크롤링한다.
 *
 * - product_id: 필수
 * - url / page_url / product_url: 선택 (호환용 - 셋 중 아무거나 채워도 됨)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AiAnalyzeRequest(

        @JsonProperty("product_id")
        String productId,

        String url,

        @JsonProperty("page_url")
        String pageUrl,

        @JsonProperty("product_url")
        String productUrl

) {
    /**
     * product_id 와 url 만으로 생성하는 편의 생성자.
     * (page_url / product_url 은 호환용으로 url 과 동일한 값을 채워 보낸다)
     */
    public static AiAnalyzeRequest of(String productId, String url) {
        return new AiAnalyzeRequest(productId, url, url, url);
    }
}

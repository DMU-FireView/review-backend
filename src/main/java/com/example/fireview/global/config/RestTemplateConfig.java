package com.example.fireview.global.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class RestTemplateConfig {

    /**
     * 기본 RestTemplate - 네이버 쇼핑 API 등 일반 용도
     * 연결 5초 / 읽기 10초
     */
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .connectTimeout(Duration.ofSeconds(5))
                .readTimeout(Duration.ofSeconds(10))
                .build();
    }

    /**
     * Data 서버 전용 RestTemplate.
     * TTL 하이브리드 조회는 저장된 값을 돌려주므로 빠르다. 크롤링을 기다리지 않는다.
     * 그래도 원격 호출이라 기본값보다는 조금 넉넉히 잡는다.
     */
    @Bean("dataRestTemplate")
    public RestTemplate dataRestTemplate(RestTemplateBuilder builder) {
        return builder
                .connectTimeout(Duration.ofSeconds(3))
                .readTimeout(Duration.ofSeconds(10))
                .build();
    }

    /**
     * AI 서버 전용 RestTemplate - 분석에 시간이 걸리므로 넉넉하게 설정
     * 연결 5초 / 읽기 30초
     */
    @Bean("aiRestTemplate")
    public RestTemplate aiRestTemplate(RestTemplateBuilder builder) {
        return builder
                .connectTimeout(Duration.ofSeconds(5))
                .readTimeout(Duration.ofSeconds(30))
                .build();
    }
}

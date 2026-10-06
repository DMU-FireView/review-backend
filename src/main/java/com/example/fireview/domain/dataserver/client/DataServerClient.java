package com.example.fireview.domain.dataserver.client;

import com.example.fireview.domain.dataserver.DataServerProductKey;
import com.example.fireview.domain.dataserver.dto.DataServerJob;
import com.example.fireview.domain.dataserver.dto.DataServerProduct;
import com.example.fireview.domain.dataserver.dto.DataServerProductResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Data 서버(review-data) 조회 클라이언트.
 *
 * <p>Data 서버는 상품·리뷰 원본의 소유자다. Spring 은 화면 조합만 하므로
 * 여기서는 읽기만 한다. 수집을 직접 트리거하는 API(/{platform}/search 등)는
 * 응답이 크롤링 속도에 묶이므로 쓰지 않는다. TTL 하이브리드 경로(/api/v1)만 쓴다.
 *
 * <p><b>실패를 예외로 올리지 않는다.</b> 상품 정보를 못 가져왔다고 챗봇이나 화면
 * 전체가 죽으면 안 된다. 호출부가 "없음"으로 다루도록 {@link Optional} 로 돌려준다.
 * 원인은 로그로 남긴다.
 */
@Slf4j
@Component
public class DataServerClient {

    private static final String TOKEN_HEADER = "X-Internal-Token";

    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final String internalToken;
    private final int reviewPageSize;

    public DataServerClient(
            @Qualifier("dataRestTemplate") RestTemplate restTemplate,
            @Value("${app.data-server.base-url:}") String baseUrl,
            @Value("${app.data-server.internal-token:}") String internalToken,
            @Value("${app.data-server.review-page-size:20}") int reviewPageSize) {

        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
        this.internalToken = internalToken;
        this.reviewPageSize = reviewPageSize;

        if (baseUrl.isBlank()) {
            log.warn("[DataServer] app.data-server.base-url 이 비어 있다. 상품 조회는 전부 빈 값으로 떨어진다");
        } else if (internalToken.isBlank()) {
            log.warn("[DataServer] app.data-server.internal-token 이 비어 있다. "
                    + "운영 Data 서버는 토큰을 요구하므로 401 이 난다 - baseUrl={}", baseUrl);
        }
    }

    public boolean isConfigured() {
        return !baseUrl.isBlank();
    }

    /**
     * 상품과 리뷰 첫 페이지를 가져온다.
     *
     * <p>202(queued)도 정상 응답이다. 수집이 끝나지 않았다는 뜻이므로 그대로 돌려주고,
     * 판단은 호출부가 한다.
     */
    public Optional<DataServerProductResponse> findProduct(DataServerProductKey key) {
        return findProduct(key, null);
    }

    /**
     * @param cursor 이전 응답의 {@code reviews.next_cursor}. 첫 페이지는 null
     */
    public Optional<DataServerProductResponse> findProduct(DataServerProductKey key, String cursor) {
        if (!isConfigured() || key == null) {
            return Optional.empty();
        }

        String uri = UriComponentsBuilder.fromUriString(baseUrl)
                .path("/api/v1/{platform}/products/{productId}")
                .queryParam("limit", reviewPageSize)
                .queryParamIfPresent("cursor", Optional.ofNullable(cursor))
                .buildAndExpand(key.platform(), key.productId())
                .toUriString();

        try {
            ResponseEntity<DataServerProductResponse> response = restTemplate.exchange(
                    uri, org.springframework.http.HttpMethod.GET,
                    new HttpEntity<>(headers()), DataServerProductResponse.class);

            DataServerProductResponse body = response.getBody();
            if (body == null) {
                log.warn("[DataServer] 응답 본문이 비었다 - {} {}", response.getStatusCode(), key.asExternalId());
                return Optional.empty();
            }
            log.debug("[DataServer] 상품 조회 - key={}, status={}", key.asExternalId(), body.status());
            return Optional.of(body);

        } catch (RestClientException e) {
            // 401(토큰), 404(미지원 플랫폼), 타임아웃 모두 여기로 온다.
            // 화면을 죽이지 않고 "데이터 없음"으로 떨어뜨린다.
            log.warn("[DataServer] 상품 조회 실패 - key={}: {}", key.asExternalId(), e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * 쇼핑몰 한 곳에서 키워드로 상품을 찾는다.
     *
     * <p>{@code GET /{platform}/search} 는 저장하지 않고 쇼핑몰을 바로 조회한다.
     * 이름만 보면 느릴 것 같지만 운영에서 재보니 컬리 240ms, 올리브영 475ms, 무신사 636ms,
     * 11번가 698ms 였다. 브라우저 기반 수집기(오늘의집·네이버 등)는 실패하거나 수십 초가
     * 걸리므로 호출할 플랫폼은 설정으로 고른다.
     *
     * <p>실패하면 빈 목록이다. 한 몰이 실패했다고 검색 전체가 실패하면 안 된다.
     */
    public List<DataServerProduct> searchProducts(String platform, String keyword, int limit) {
        if (!isConfigured() || platform == null || keyword == null || keyword.isBlank()) {
            return List.of();
        }
        // URI 객체로 넘겨야 RestTemplate 이 다시 인코딩하지 않는다. 문자열로 넘기면
        // 한글 키워드의 % 가 %25 로 이중 인코딩된다.
        URI uri = UriComponentsBuilder.fromUriString(baseUrl)
                .path("/{platform}/search")
                .queryParam("keyword", keyword)
                .queryParam("limit", limit)
                .buildAndExpand(platform)
                .encode()
                .toUri();
        try {
            DataServerProduct[] body = restTemplate.exchange(
                    uri, org.springframework.http.HttpMethod.GET,
                    new HttpEntity<>(headers()), DataServerProduct[].class).getBody();
            return body == null ? List.of() : Arrays.asList(body);
        } catch (RestClientException e) {
            log.warn("[DataServer] 검색 실패 - platform={}, keyword={}: {}", platform, keyword, e.getMessage());
            return List.of();
        }
    }

    /** 수집 job 상태. 상품 조회가 queued 를 줬을 때 진행 상황을 본다 */
    public Optional<DataServerJob> findJob(long jobId) {
        if (!isConfigured()) {
            return Optional.empty();
        }
        String uri = UriComponentsBuilder.fromUriString(baseUrl)
                .path("/api/v1/jobs/{jobId}")
                .buildAndExpand(jobId)
                .toUriString();
        try {
            return Optional.ofNullable(restTemplate.exchange(
                    uri, org.springframework.http.HttpMethod.GET,
                    new HttpEntity<>(headers()), DataServerJob.class).getBody());
        } catch (RestClientException e) {
            log.warn("[DataServer] job 조회 실패 - jobId={}: {}", jobId, e.getMessage());
            return Optional.empty();
        }
    }

    private HttpHeaders headers() {
        HttpHeaders headers = new HttpHeaders();
        if (!internalToken.isBlank()) {
            headers.set(TOKEN_HEADER, internalToken);
        }
        return headers;
    }
}

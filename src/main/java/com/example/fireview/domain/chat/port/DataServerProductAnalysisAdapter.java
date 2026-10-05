package com.example.fireview.domain.chat.port;

import com.example.fireview.domain.dataserver.DataServerProductKey;
import com.example.fireview.domain.dataserver.client.DataServerClient;
import com.example.fireview.domain.dataserver.dto.DataServerProductResponse;
import com.example.fireview.domain.dataserver.dto.DataServerReview;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Data 서버에서 챗봇 컨텍스트를 가져오는 어댑터.
 *
 * <p>{@code app.chat.stub-analysis.enabled=false} 일 때 {@link StubProductAnalysisAdapter}
 * 대신 이 빈이 쓰인다.
 *
 * <p><b>RTI·등급·장단점은 채우지 않는다(null).</b> Data 서버는 원본 수집만 소유하고
 * 분석 결과를 주지 않는다. 비워두면 {@code PromptAssembler} 가 신뢰등급 항목을 아예
 * 싣지 않고, 모델은 시스템 프롬프트 규칙에 따라 "데이터에 없다"고 답한다.
 * 여기서 0 이나 임의값을 넣으면 모델이 그 수치를 사실처럼 말한다.
 *
 * <p>분석 결과가 붙는 시점(Data 서버의 analysis_jobs 또는 AI 서버 경로)에
 * 이 자리만 채우면 챗봇은 그대로 동작한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.chat.stub-analysis.enabled", havingValue = "false")
public class DataServerProductAnalysisAdapter implements ProductAnalysisPort {

    /** 프롬프트에 실을 대표 리뷰 수. 토큰 예산 때문에 전부 넣지 않는다 */
    private static final int MAX_SAMPLES = 5;

    private final DataServerClient dataServerClient;

    @Override
    public Optional<ProductAnalysisContext> findContext(String productId) {
        Optional<DataServerProductKey> key = DataServerProductKey.parse(productId);
        if (key.isEmpty()) {
            log.debug("[Chat] 상품 식별자 형식이 아니다 - productId={}", productId);
            return Optional.empty();
        }

        Optional<DataServerProductResponse> found = dataServerClient.findProduct(key.get());
        if (found.isEmpty()) {
            return Optional.empty();
        }

        DataServerProductResponse response = found.get();
        if (!response.hasUsableData()) {
            // queued. 아직 수집 전이라 할 말이 없다. 빈 컨텍스트로 가면
            // 챗봇이 "해당 상품 정보를 아직 모른다"고 답한다.
            log.debug("[Chat] 수집 대기 중인 상품 - key={}, job={}",
                    key.get().asExternalId(),
                    response.job() != null ? response.job().id() : null);
            return Optional.empty();
        }

        return Optional.of(toContext(productId, response));
    }

    private ProductAnalysisContext toContext(String externalId, DataServerProductResponse response) {
        var product = response.product();
        List<DataServerReview> reviews = response.reviewItems();

        // 쇼핑몰이 알려준 전체 리뷰 수를 우선 쓴다. 우리가 이번에 받아온 건 한 페이지뿐이라
        // 그 수를 쓰면 표본이 실제보다 적어 보여 '판단 보류'가 과하게 걸린다.
        int totalReviews = product.reviewCount() != null ? product.reviewCount() : reviews.size();

        return new ProductAnalysisContext(
                externalId,
                product.name(),
                product.price(),
                product.category(),
                null,   // averageRti  — Data 서버는 분석 결과를 주지 않는다
                null,   // trustGrade  — 위와 같음
                totalReviews,
                List.of(),  // pros
                List.of(),  // cons
                List.of(),  // trustSignals
                toSamples(reviews));
    }

    private List<ProductAnalysisContext.SampleReview> toSamples(List<DataServerReview> reviews) {
        return reviews.stream()
                .filter(r -> r.content() != null && !r.content().isBlank())
                .limit(MAX_SAMPLES)
                .map(r -> new ProductAnalysisContext.SampleReview(
                        r.content(),
                        // Data 서버 평점은 NUMERIC(3,2) 라 4.5 가 올 수 있다
                        r.rating() == null ? null : (int) Math.round(r.rating()),
                        null,   // rti   — 리뷰별 분석 결과 없음
                        null))  // grade
                .toList();
    }
}

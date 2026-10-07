package com.example.fireview.domain.dataserver.service;

import com.example.fireview.domain.dataserver.DataServerFixtures;
import com.example.fireview.domain.dataserver.client.DataServerClient;
import com.example.fireview.domain.dataserver.dto.response.AnalysisStatus;
import com.example.fireview.domain.dataserver.dto.response.CollectionStatus;
import com.example.fireview.domain.dataserver.dto.response.DataProductResponse;
import com.example.fireview.domain.dataserver.dto.response.DataProductResponse.DataReview;
import com.example.fireview.domain.product.repository.ProductRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/** Data 서버 analysis 를 v2 상세 응답으로 옮기는 부분 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DataProductServiceAnalysisTest {

    @Mock DataServerClient dataServerClient;
    @Mock ProductRepository productRepository;
    @Mock ProductTagRegistry registry;
    @InjectMocks DataProductService service;

    private DataProductResponse callWith(String fixture) {
        when(dataServerClient.findProduct(any(), any()))
                .thenReturn(Optional.of(DataServerFixtures.load(fixture)));
        when(registry.find(any())).thenReturn(Optional.empty());
        return service.getProduct("kurly", "1000146248", null);
    }

    private static DataReview review(DataProductResponse res, String reviewId) {
        return res.reviews().items().stream()
                .filter(r -> r.reviewId().equals(reviewId))
                .findFirst().orElseThrow();
    }

    @Test
    void done이면_상태와_버전을_내려준다() {
        DataProductResponse res = callWith("product-analysis-done.json");

        assertThat(res.analysis().status()).isEqualTo(AnalysisStatus.DONE);
        assertThat(res.analysis().modelVersion()).isEqualTo("rti-model-0.5");
        assertThat(res.analysis().policyVersion()).isEqualTo("rti-v0");
        assertThat(res.analysis().reviewCount()).isEqualTo(128);
    }

    @Test
    void 리뷰마다_review_id로_결과를_붙인다() {
        DataProductResponse res = callWith("product-analysis-done.json");

        DataReview r1 = review(res, "r-1");
        assertThat(r1.rti()).isEqualTo(82.5);
        assertThat(r1.level()).isEqualTo("safe");   // Data 원문 그대로. 한국어로 바꾸지 않는다
        assertThat(r1.reasons()).containsExactly("TEXT_SPECIFIC_DETAIL");
    }

    @Test
    void 결과가_없는_리뷰는_0이_아니라_null이다() {
        DataProductResponse res = callWith("product-analysis-done.json");

        DataReview r2 = review(res, "r-2");   // results 에 아예 없음
        assertThat(r2.rti()).isNull();
        assertThat(r2.level()).isNull();
        assertThat(r2.reasons()).isEmpty();

        DataReview r3 = review(res, "r-3");   // 결과는 있지만 계산 불가
        assertThat(r3.rti()).isNull();
        assertThat(r3.level()).isNull();
    }

    @Test
    void 분석_전_상태는_리뷰에_점수가_없다() {
        for (String fixture : List.of("product-analysis-not-analyzed.json",
                "product-analysis-disabled.json", "product-analysis-stale.json")) {
            DataProductResponse res = callWith(fixture);

            assertThat(res.reviews().items()).hasSize(3)
                    .allSatisfy(r -> {
                        assertThat(r.rti()).isNull();
                        assertThat(r.level()).isNull();
                        assertThat(r.reasons()).isEmpty();
                    });
        }
    }

    @Test
    void Data_상태값을_그대로_올린다() {
        DataProductResponse notAnalyzed = callWith("product-analysis-not-analyzed.json");
        assertThat(notAnalyzed.analysis().status()).isEqualTo(AnalysisStatus.NOT_ANALYZED);
        assertThat(notAnalyzed.analysis().modelVersion()).isNull();
        assertThat(notAnalyzed.analysis().reviewCount()).isZero();

        DataProductResponse disabled = callWith("product-analysis-disabled.json");
        assertThat(disabled.analysis().status()).isEqualTo(AnalysisStatus.DISABLED);
        // 수집 상태(stale)와 분석 상태는 따로 간다
        assertThat(disabled.collectionStatus()).isEqualTo(CollectionStatus.STALE);
        assertThat(disabled.job().id()).isEqualTo(7L);

        DataProductResponse stale = callWith("product-analysis-stale.json");
        assertThat(stale.analysis().status()).isEqualTo(AnalysisStatus.STALE);
        assertThat(stale.analysis().modelVersion()).isEqualTo("rti-model-0.4");
    }

    @Test
    void analysis가_없는_구버전_응답은_UNAVAILABLE이다() {
        DataProductResponse res = callWith("product-analysis-missing.json");

        assertThat(res.analysis().status()).isEqualTo(AnalysisStatus.UNAVAILABLE);
        assertThat(res.analysis().modelVersion()).isNull();
        assertThat(res.analysis().reviewCount()).isNull();
        assertThat(res.reviews().items()).hasSize(3).allSatisfy(r -> assertThat(r.rti()).isNull());
    }

    @Test
    void 수집_대기면_상품이_없고_분석_상태만_있다() {
        DataProductResponse res = callWith("product-queued.json");

        assertThat(res.collectionStatus()).isEqualTo(CollectionStatus.QUEUED);
        assertThat(res.product()).isNull();
        assertThat(res.reviews().items()).isEmpty();
        assertThat(res.job().id()).isEqualTo(9L);
        assertThat(res.analysis().status()).isEqualTo(AnalysisStatus.NOT_ANALYZED);
    }

    @Test
    void 모르는_상태값은_UNAVAILABLE이다() {
        assertThat(AnalysisStatus.from("archived")).isEqualTo(AnalysisStatus.UNAVAILABLE);
        assertThat(AnalysisStatus.from(null)).isEqualTo(AnalysisStatus.UNAVAILABLE);
    }

    @Test
    void JSON_응답_모양() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode json = mapper.valueToTree(callWith("product-analysis-done.json"));

        JsonNode analysis = json.get("analysis");
        assertThat(analysis.get("status").asText()).isEqualTo("DONE");
        assertThat(analysis.get("modelVersion").asText()).isEqualTo("rti-model-0.5");
        assertThat(analysis.get("policyVersion").asText()).isEqualTo("rti-v0");
        assertThat(analysis.get("reviewCount").asInt()).isEqualTo(128);
        // 상품 평균은 내려주지 않는다 — 한 페이지 표본 평균이 된다
        assertThat(analysis.has("averageRti")).isFalse();
        assertThat(analysis.has("results")).isFalse();

        JsonNode first = json.get("reviews").get("items").get(0);
        assertThat(first.get("rti").asDouble()).isEqualTo(82.5);
        assertThat(first.get("level").asText()).isEqualTo("safe");
        assertThat(first.get("reasons").isArray()).isTrue();

        JsonNode second = json.get("reviews").get("items").get(1);
        assertThat(second.get("rti").isNull()).isTrue();
        assertThat(second.get("level").isNull()).isTrue();
        assertThat(second.get("reasons")).isEmpty();
    }

    @Test
    void JSON_응답_모양_분석_없음() {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode json = mapper.valueToTree(callWith("product-analysis-missing.json"));

        // null 이 아니라 상태 객체가 온다
        assertThat(json.get("analysis").get("status").asText()).isEqualTo("UNAVAILABLE");
        assertThat(json.get("analysis").get("modelVersion").isNull()).isTrue();
    }
}

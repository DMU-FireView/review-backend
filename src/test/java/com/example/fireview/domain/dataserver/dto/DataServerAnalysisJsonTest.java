package com.example.fireview.domain.dataserver.dto;

import com.example.fireview.domain.dataserver.DataServerFixtures;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DataServerAnalysisJsonTest {

    @Test
    void done이면_버전과_리뷰별_결과를_읽는다() {
        DataServerAnalysis analysis = DataServerFixtures.load("product-analysis-done.json").analysis();

        assertThat(analysis.status()).isEqualTo("done");
        assertThat(analysis.modelVersion()).isEqualTo("rti-model-0.5");
        assertThat(analysis.policyVersion()).isEqualTo("rti-v0");
        assertThat(analysis.inputHash()).isEqualTo("sha256:abc");
        assertThat(analysis.reviewCount()).isEqualTo(128);
        assertThat(analysis.job().id()).isEqualTo(31L);
        assertThat(analysis.job().aiJobId()).isEqualTo("ai-7f3a");
        assertThat(analysis.job().inputReviewCount()).isEqualTo(128);
        assertThat(analysis.results()).hasSize(2);

        DataServerReviewAnalysis r1 = analysis.resultsByReviewId().get("r-1");
        assertThat(r1.rti()).isEqualTo(82.5);
        assertThat(r1.level()).isEqualTo("safe");
        assertThat(r1.textScore()).isEqualTo(80.0);
        assertThat(r1.behaviorScore()).isNull();
        assertThat(r1.networkScore()).isEqualTo(90.0);
        assertThat(r1.reasons()).containsExactly("TEXT_SPECIFIC_DETAIL");
    }

    @Test
    void 계산_불가는_null로_남는다() {
        // Data 서버는 AI 의 -1 을 null 로 바꿔 보낸다. 0 이나 -1 이 되면 안 된다
        DataServerReviewAnalysis r3 = DataServerFixtures.load("product-analysis-done.json")
                .analysis().resultsByReviewId().get("r-3");

        assertThat(r3.rti()).isNull();
        assertThat(r3.level()).isNull();
        assertThat(r3.textScore()).isNull();
        assertThat(r3.reasons()).isEmpty();
    }

    @Test
    void AI_원본처럼_음수가_와도_null로_바꾼다() throws JsonProcessingException {
        DataServerReviewAnalysis r = DataServerFixtures.MAPPER.readValue("""
                {"review_id": "r-9", "rti": -1, "level": null, "text_score": -1,
                 "behavior_score": -1, "network_score": 55.0, "reasons": null}
                """, DataServerReviewAnalysis.class);

        assertThat(r.rti()).isNull();
        assertThat(r.textScore()).isNull();
        assertThat(r.behaviorScore()).isNull();
        assertThat(r.networkScore()).isEqualTo(55.0);
        assertThat(r.reasons()).isEmpty();
    }

    @Test
    void job이_없는_상태는_결과가_비어_있다() {
        for (String name : new String[]{"product-analysis-not-analyzed.json", "product-analysis-disabled.json"}) {
            DataServerAnalysis analysis = DataServerFixtures.load(name).analysis();

            assertThat(analysis.job()).isNull();
            assertThat(analysis.modelVersion()).isNull();
            assertThat(analysis.reviewCount()).isZero();
            assertThat(analysis.results()).isEmpty();
            assertThat(analysis.resultsByReviewId()).isEmpty();
        }
        assertThat(DataServerFixtures.load("product-analysis-not-analyzed.json").analysis().status())
                .isEqualTo("not_analyzed");
        assertThat(DataServerFixtures.load("product-analysis-disabled.json").analysis().status())
                .isEqualTo("disabled");
    }

    @Test
    void stale은_job은_있고_결과는_없다() {
        DataServerAnalysis analysis = DataServerFixtures.load("product-analysis-stale.json").analysis();

        assertThat(analysis.status()).isEqualTo("stale");
        assertThat(analysis.job().status()).isEqualTo("done");
        assertThat(analysis.results()).isEmpty();
    }

    @Test
    void 구버전처럼_analysis가_없으면_null이다() {
        DataServerProductResponse body = DataServerFixtures.load("product-analysis-missing.json");

        assertThat(body.analysis()).isNull();
        assertThat(body.product().name()).isEqualTo("샘플 상품");
        assertThat(body.reviewItems()).hasSize(3);
    }

    @Test
    void 결과_목록이_null이어도_빈_목록이다() throws JsonProcessingException {
        DataServerAnalysis analysis = DataServerFixtures.MAPPER.readValue(
                "{\"status\": \"running\"}", DataServerAnalysis.class);

        assertThat(analysis.results()).isEmpty();
        assertThat(analysis.reviewCount()).isNull();
    }

    @Test
    void 표본_정보를_읽는다() {
        // review_count 는 분석 입력(표본) 수, source_review_count 는 Data 가 가진 원본 리뷰 수
        DataServerAnalysis analysis = DataServerFixtures.load("product-analysis-done.json").analysis();

        assertThat(analysis.sampled()).isTrue();
        assertThat(analysis.sourceReviewCount()).isEqualTo(1318);
        assertThat(analysis.reviewCount()).isEqualTo(128);
    }

    @Test
    void 표본_필드가_없는_구버전_done은_null로_둔다() throws JsonProcessingException {
        // Data #80 이전 응답. 모르는 값을 false·0 으로 채우면 "전수 분석"으로 읽힌다
        DataServerAnalysis analysis = DataServerFixtures.MAPPER.readValue("""
                {"status": "done", "model_version": "m", "policy_version": "rti-v0",
                 "review_count": 40, "results": []}
                """, DataServerAnalysis.class);

        assertThat(analysis.sampled()).isNull();
        assertThat(analysis.sourceReviewCount()).isNull();
        assertThat(analysis.reviewCount()).isEqualTo(40);
    }
}

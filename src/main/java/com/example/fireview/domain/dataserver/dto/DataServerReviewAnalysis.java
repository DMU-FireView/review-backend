package com.example.fireview.domain.dataserver.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * 리뷰 한 건의 신뢰도 분석 결과 ({@code analysis.results[]}).
 *
 * <p>모든 점수는 0~100 이고 <b>null 은 "계산 불가"</b>다. AI 는 계산 불가를 -1 로 보내지만
 * Data 서버가 저장하면서 null 로 바꾼다. 그래도 음수가 들어오면 null 로 다룬다 —
 * -1 을 숫자로 두면 화면·챗봇이 "RTI -1" 을 실제 점수처럼 보여준다.
 *
 * @param level  {@code safe}(≥70) / {@code warn}(≥40) / {@code danger}(&lt;40). RTI 가 계산 불가면 null
 * @param reasons 판단 근거. 코드({@code TEXT_*} 등)와 한국어 문장이 섞여 올 수 있다
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record DataServerReviewAnalysis(
        @JsonProperty("review_id") String reviewId,
        Double rti,
        String level,
        @JsonProperty("text_score") Double textScore,
        @JsonProperty("behavior_score") Double behaviorScore,
        @JsonProperty("network_score") Double networkScore,
        List<String> reasons
) {

    public DataServerReviewAnalysis {
        rti = knownScore(rti);
        textScore = knownScore(textScore);
        behaviorScore = knownScore(behaviorScore);
        networkScore = knownScore(networkScore);
        reasons = reasons == null ? List.of() : reasons;
    }

    private static Double knownScore(Double score) {
        return score == null || score < 0 ? null : score;
    }
}

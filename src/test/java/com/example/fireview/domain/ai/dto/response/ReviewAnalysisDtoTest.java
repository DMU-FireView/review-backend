package com.example.fireview.domain.ai.dto.response;

import com.example.fireview.global.response.ReasonMessages;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Data 서버가 빈 사유 배열을 줬을 때 프론트 응답이 어떻게 나가는지 고정한다.
 */
class ReviewAnalysisDtoTest {

    private static SampleReview sample(List<AiReason> reasons) {
        return new SampleReview("r-1", "user**", "2026.09.30", 5, "좋아요", "danger", reasons);
    }

    private static AiAnalysisResult result(List<AiReason> reasons) {
        return new AiAnalysisResult("r-1", "p-1", "u-1", 5, "2026-09-30",
                30, "danger", null, reasons, null);
    }

    @Test
    void 사유가_빈_배열이면_안내_문구가_내려간다() {
        ReviewAnalysisDto dto = ReviewAnalysisDto.from(sample(List.of()), null);

        assertThat(dto.reasons()).containsExactly(ReasonMessages.NO_DETAIL);
    }

    @Test
    void 사유가_null이어도_안내_문구가_내려간다() {
        ReviewAnalysisDto dto = ReviewAnalysisDto.from(sample(null), null);

        assertThat(dto.reasons()).containsExactly(ReasonMessages.NO_DETAIL);
    }

    @Test
    void 사유가_있으면_메시지만_뽑아_그대로_내려간다() {
        ReviewAnalysisDto dto = ReviewAnalysisDto.from(
                sample(List.of(new AiReason("EXCESSIVE_EXCLAMATION", "과도한 느낌표 사용"),
                               new AiReason("SHORT_CONTENT", "리뷰 내용이 매우 짧습니다"))), null);

        assertThat(dto.reasons())
                .containsExactly("과도한 느낌표 사용", "리뷰 내용이 매우 짧습니다")
                .doesNotContain(ReasonMessages.NO_DETAIL);
    }

    @Test
    void 메시지가_비어있는_사유만_오면_안내_문구로_바뀐다() {
        // 코드만 있고 message 가 비어 있는 경우. 화면에 빈 태그가 생기면 안 된다
        ReviewAnalysisDto dto = ReviewAnalysisDto.from(
                sample(List.of(new AiReason("UNKNOWN", ""), new AiReason("UNKNOWN2", null))), null);

        assertThat(dto.reasons()).containsExactly(ReasonMessages.NO_DETAIL);
    }

    @Test
    void 상품상세_경로에서도_똑같이_동작한다() {
        assertThat(ReviewAnalysisDto.from(result(List.of())).reasons())
                .containsExactly(ReasonMessages.NO_DETAIL);
        assertThat(ReviewAnalysisDto.from(result(null)).reasons())
                .containsExactly(ReasonMessages.NO_DETAIL);
        assertThat(ReviewAnalysisDto.from(result(List.of(new AiReason("AD", "광고성 문구")))).reasons())
                .containsExactly("광고성 문구");
    }
}

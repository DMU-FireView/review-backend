package com.example.fireview.global.response;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ReasonMessagesTest {

    @Test
    void 빈_배열이면_안내_문구로_바꾼다() {
        assertThat(ReasonMessages.orPlaceholder(List.of()))
                .containsExactly(ReasonMessages.NO_DETAIL);
    }

    @Test
    void null도_빈_배열과_같게_다룬다() {
        assertThat(ReasonMessages.orPlaceholder(null))
                .containsExactly(ReasonMessages.NO_DETAIL);
    }

    @Test
    void 사유가_있으면_그대로_둔다() {
        List<String> reasons = List.of("광고성 문구가 감지되었습니다.", "리뷰 내용이 매우 짧습니다.");

        assertThat(ReasonMessages.orPlaceholder(reasons)).isEqualTo(reasons);
    }

    @Test
    void 공백만_든_항목은_걸러낸다() {
        assertThat(ReasonMessages.orPlaceholder(Arrays.asList("  ", null, "광고성 문구")))
                .containsExactly("광고성 문구");
    }

    @Test
    void 걸러내고_나서_비면_안내_문구로_바꾼다() {
        // 데이터 서버가 메시지 없는 사유만 보냈을 때. 빈 배열과 같게 다뤄야 한다
        assertThat(ReasonMessages.orPlaceholder(Arrays.asList("", "   ", null)))
                .containsExactly(ReasonMessages.NO_DETAIL);
    }

    @Test
    void 원본_목록을_건드리지_않는다() {
        List<String> original = new ArrayList<>(List.of("광고성 문구"));

        ReasonMessages.orPlaceholder(original);

        assertThat(original).containsExactly("광고성 문구");
    }
}

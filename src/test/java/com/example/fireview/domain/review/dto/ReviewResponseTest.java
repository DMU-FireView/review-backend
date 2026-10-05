package com.example.fireview.domain.review.dto;

import com.example.fireview.domain.product.entity.Product;
import com.example.fireview.domain.review.entity.Review;
import com.example.fireview.domain.review.entity.TrustGrade;
import com.example.fireview.global.response.ReasonMessages;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ReviewResponseTest {

    private static Review review(List<String> reasons) {
        Review r = Review.builder()
                .product(Product.builder().id(1L).name("샘플 상품").build())
                .reviewerNickname("익명")
                .content("좋아요")
                .rating(5)
                .rtiScore(30.0)
                .trustGrade(TrustGrade.DANGER)
                .writtenAt(java.time.LocalDateTime.now())
                .isVerifiedPurchase(false)
                .build();
        r.setReasons(reasons);
        return r;
    }

    @Test
    void 저장된_사유가_없으면_안내_문구로_내보낸다() {
        assertThat(ReviewResponse.from(review(List.of())).reasons())
                .containsExactly(ReasonMessages.NO_DETAIL);
    }

    @Test
    void 저장된_사유가_있으면_그대로_내보낸다() {
        List<String> reasons = List.of("광고성 문구 또는 반복 표현이 감지되었습니다.");

        assertThat(ReviewResponse.from(review(reasons)).reasons()).isEqualTo(reasons);
    }

    @Test
    void 치환은_응답에서만_일어나고_엔티티는_비어_있는_채로_둔다() {
        // 저장까지 문장이 들어가면 "사유 없음" 과 "사유 있음" 을 구분할 수 없고
        // 사유 집계에 이 문장이 한 건으로 섞인다
        Review entity = review(List.of());

        ReviewResponse response = ReviewResponse.from(entity);

        assertThat(response.reasons()).containsExactly(ReasonMessages.NO_DETAIL);
        assertThat(entity.getReasons()).isEmpty();
    }
}

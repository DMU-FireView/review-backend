package com.example.fireview.domain.review.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TrustGradeTest {

    @Test
    void 경계는_AI_정책과_같은_70과_40이다() {
        // AI rti-v0·Data 서버: safe ≥70 / warn ≥40 / danger <40
        assertThat(TrustGrade.fromScore(100)).isEqualTo(TrustGrade.SAFE);
        assertThat(TrustGrade.fromScore(70)).isEqualTo(TrustGrade.SAFE);
        assertThat(TrustGrade.fromScore(69.9)).isEqualTo(TrustGrade.SUSPICIOUS);
        assertThat(TrustGrade.fromScore(40)).isEqualTo(TrustGrade.SUSPICIOUS);
        assertThat(TrustGrade.fromScore(39.9)).isEqualTo(TrustGrade.DANGER);
        assertThat(TrustGrade.fromScore(0)).isEqualTo(TrustGrade.DANGER);
    }

    @Test
    void 정수_RTI도_같은_경계를_쓴다() {
        assertThat(TrustGrade.fromRti(70)).isEqualTo(TrustGrade.SAFE);
        assertThat(TrustGrade.fromRti(69)).isEqualTo(TrustGrade.SUSPICIOUS);
        assertThat(TrustGrade.fromRti(40)).isEqualTo(TrustGrade.SUSPICIOUS);
        assertThat(TrustGrade.fromRti(39)).isEqualTo(TrustGrade.DANGER);
    }

    @Test
    void AI_level을_그대로_옮긴다() {
        assertThat(TrustGrade.fromAiLevel("safe")).isEqualTo(TrustGrade.SAFE);
        assertThat(TrustGrade.fromAiLevel("warn")).isEqualTo(TrustGrade.SUSPICIOUS);
        assertThat(TrustGrade.fromAiLevel("DANGER")).isEqualTo(TrustGrade.DANGER);
    }

    @Test
    void level이_null이면_판단_불가라_등급이_없다() {
        // 예전에는 SUSPICIOUS 로 바뀌어 "판단 불가"가 "의심"으로 보였다
        assertThat(TrustGrade.fromAiLevel(null)).isNull();
    }

    @Test
    void level_문자열로_되돌린다() {
        for (TrustGrade grade : TrustGrade.values()) {
            assertThat(TrustGrade.fromAiLevel(grade.toLevel())).isEqualTo(grade);
        }
    }
}

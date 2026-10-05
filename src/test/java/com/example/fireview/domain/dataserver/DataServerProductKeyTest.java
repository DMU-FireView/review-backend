package com.example.fireview.domain.dataserver;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DataServerProductKeyTest {

    @Test
    void 플랫폼과_상품ID로_가른다() {
        assertThat(DataServerProductKey.parse("naver-7195971829"))
                .contains(new DataServerProductKey("naver", "7195971829"));
    }

    @Test
    void 상품ID에_하이픈이_있어도_첫_하이픈에서만_자른다() {
        assertThat(DataServerProductKey.parse("kurly-1000-146248"))
                .contains(new DataServerProductKey("kurly", "1000-146248"));
    }

    @Test
    void 형식이_아니면_비운다() {
        assertThat(DataServerProductKey.parse(null)).isEmpty();
        assertThat(DataServerProductKey.parse("")).isEmpty();
        assertThat(DataServerProductKey.parse("   ")).isEmpty();
        assertThat(DataServerProductKey.parse("7195971829")).isEmpty();   // 하이픈 없음
        assertThat(DataServerProductKey.parse("-7195971829")).isEmpty();  // 플랫폼 없음
        assertThat(DataServerProductKey.parse("naver-")).isEmpty();       // 상품 ID 없음
    }

    @Test
    void 다시_한_문자열로_되돌린다() {
        String raw = "elevenst-123456";

        assertThat(DataServerProductKey.parse(raw))
                .get()
                .extracting(DataServerProductKey::asExternalId)
                .isEqualTo(raw);
    }
}

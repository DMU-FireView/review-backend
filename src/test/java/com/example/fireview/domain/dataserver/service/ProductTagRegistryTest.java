package com.example.fireview.domain.dataserver.service;

import com.example.fireview.domain.dataserver.DataServerProductKey;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProductTagRegistryTest {

    private static final DataServerProductKey KEY =
            new DataServerProductKey("kurly", "1000146248");

    @Test
    void 같은_키는_늘_같은_ID를_준다() {
        // 결정론적이라 동시 요청이 서로 다른 ID 로 두 행을 만들려는 경합이 없다
        assertThat(ProductTagRegistry.allocateId(KEY))
                .isEqualTo(ProductTagRegistry.allocateId(
                        new DataServerProductKey("kurly", "1000146248")));
    }

    @Test
    void 다른_키는_다른_ID를_준다() {
        assertThat(ProductTagRegistry.allocateId(KEY))
                .isNotEqualTo(ProductTagRegistry.allocateId(
                        new DataServerProductKey("elevenst", "1000146248")))
                .isNotEqualTo(ProductTagRegistry.allocateId(
                        new DataServerProductKey("kurly", "1000146249")));
    }

    @Test
    void ID는_항상_양수다() {
        for (String platform : new String[]{"kurly", "naver", "elevenst", "ably", "musinsa"}) {
            for (int i = 0; i < 200; i++) {
                assertThat(ProductTagRegistry.allocateId(
                        new DataServerProductKey(platform, String.valueOf(i))))
                        .isPositive();
            }
        }
    }

    @Test
    void 더미_ID_구간을_비켜간다() {
        // 기존 더미 33건이 900000000000~ 를 쓴다. 겹치면 같은 행을 가리키게 된다
        for (int i = 0; i < 2000; i++) {
            long id = ProductTagRegistry.allocateId(new DataServerProductKey("kurly", "p" + i));
            assertThat(id < 900_000_000_000L || id >= 900_001_000_000L)
                    .as("더미 구간에 떨어진 ID: %d", id)
                    .isTrue();
        }
    }

    @Test
    void 대량으로_뽑아도_충돌하지_않는다() {
        java.util.Set<Long> ids = new java.util.HashSet<>();
        for (int i = 0; i < 20_000; i++) {
            ids.add(ProductTagRegistry.allocateId(new DataServerProductKey("kurly", String.valueOf(i))));
        }
        assertThat(ids).hasSize(20_000);
    }
}

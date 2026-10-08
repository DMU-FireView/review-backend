package com.example.fireview.domain.dataserver.service;

import com.example.fireview.domain.dataserver.DataServerProductKey;
import com.example.fireview.domain.dataserver.dto.response.AnalysisStatus;
import com.example.fireview.domain.product.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

/**
 * 분석 상태가 실제로 바뀌었을 때만 홈 목록 캐시를 비우는지 본다.
 * 같은 상태를 다시 볼 때마다 캐시를 비우면 상세 조회가 홈 캐시를 계속 날린다.
 */
@SpringJUnitConfig(ProductAnalysisStatusCacheTest.Config.class)
class ProductAnalysisStatusCacheTest {

    private static final DataServerProductKey KEY = new DataServerProductKey("kurly", "1000146248");

    @Configuration
    @EnableCaching
    @Import(ProductTagRegistry.class)
    static class Config {
        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager("productList");
        }
    }

    @Autowired ProductTagRegistry registry;
    @Autowired CacheManager cacheManager;
    @MockitoBean ProductRepository productRepository;

    private Cache homeList;

    @BeforeEach
    void setUp() {
        homeList = cacheManager.getCache("productList");
        homeList.put("all", "cached");
    }

    /** 칼럼 지정 UPDATE 가 바꾼 행 수. 같은 상태거나 번호표가 없으면 0 이다 */
    private void updatedRows(int rows) {
        given(productRepository.updateAnalysisStatus(anyString(), anyString(), anyString(), any()))
                .willReturn(rows);
    }

    @Test
    void 상태가_바뀌면_홈_목록_캐시를_비운다() {
        updatedRows(1);

        registry.recordAnalysisStatus(KEY, AnalysisStatus.DONE);

        assertThat(homeList.get("all")).isNull();
    }

    @Test
    void 같은_상태면_캐시를_그대로_둔다() {
        updatedRows(0);

        registry.recordAnalysisStatus(KEY, AnalysisStatus.DONE);

        assertThat(homeList.get("all")).isNotNull();
    }

    @Test
    void UNAVAILABLE이면_캐시를_그대로_둔다() {
        registry.recordAnalysisStatus(KEY, AnalysisStatus.UNAVAILABLE);

        assertThat(homeList.get("all")).isNotNull();
    }
}

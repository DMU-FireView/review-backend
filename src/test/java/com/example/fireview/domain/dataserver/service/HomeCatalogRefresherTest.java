package com.example.fireview.domain.dataserver.service;

import com.example.fireview.domain.product.dto.ProductResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HomeCatalogRefresherTest {

    private DataProductCatalogService catalog;

    @BeforeEach
    void setUp() {
        catalog = mock(DataProductCatalogService.class);
        when(catalog.isEnabled()).thenReturn(true);
    }

    private HomeCatalogRefresher refresher(String keywords) {
        return new HomeCatalogRefresher(catalog, Runnable::run, keywords, 3, 0, false);
    }

    private static List<ProductResponse> results(int n) {
        return java.util.Collections.nCopies(n, (ProductResponse) null);
    }

    @Test
    void 키워드마다_몰당_건수를_줄여_검색한다() {
        when(catalog.search(anyString(), anyInt())).thenReturn(results(12));

        int total = refresher("선크림, 라면 ,이어폰").refresh();

        verify(catalog).search("선크림", 3);
        verify(catalog).search("라면", 3);
        verify(catalog).search("이어폰", 3);
        assertThat(total).isEqualTo(36);
    }

    @Test
    void 한_키워드가_실패해도_나머지는_계속한다() {
        when(catalog.search(eq("선크림"), anyInt())).thenThrow(new IllegalStateException("timeout"));
        when(catalog.search(eq("라면"), anyInt())).thenReturn(results(5));

        int total = refresher("선크림,라면").refresh();

        verify(catalog).search("라면", 3);
        assertThat(total).isEqualTo(5);
    }

    @Test
    void Data_서버를_쓸_수_없으면_아무것도_부르지_않는다() {
        when(catalog.isEnabled()).thenReturn(false);

        refresher("선크림").refresh();

        verify(catalog, never()).search(anyString(), anyInt());
    }

    @Test
    void 이미_돌고_있으면_겹쳐_돌지_않는다() throws Exception {
        // 배포 직후 실행과 정기 실행이 겹치면 같은 검색을 두 번 보낸다
        CountDownLatch inSearch = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        when(catalog.search(anyString(), anyInt())).thenAnswer(inv -> {
            inSearch.countDown();
            release.await(2, TimeUnit.SECONDS);
            return results(1);
        });
        HomeCatalogRefresher refresher = refresher("선크림");
        var pool = Executors.newSingleThreadExecutor();
        try {
            Future<Integer> first = pool.submit(refresher::refresh);
            assertThat(inSearch.await(2, TimeUnit.SECONDS)).isTrue();

            assertThat(refresher.refresh()).isEqualTo(-1);

            release.countDown();
            assertThat(first.get(2, TimeUnit.SECONDS)).isEqualTo(1);
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void 배포_직후_실행은_설정으로_끌_수_있다() {
        new HomeCatalogRefresher(catalog, Runnable::run, "선크림", 3, 0, false).onStartup();
        verify(catalog, never()).search(anyString(), anyInt());

        when(catalog.search(anyString(), anyInt())).thenReturn(results(1));
        new HomeCatalogRefresher(catalog, Runnable::run, "선크림", 3, 0, true).onStartup();
        verify(catalog).search("선크림", 3);
    }
}

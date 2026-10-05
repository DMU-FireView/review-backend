package com.example.fireview.domain.dataserver.service;

import com.example.fireview.domain.dataserver.dto.DataServerJob;
import com.example.fireview.domain.dataserver.dto.DataServerProduct;
import com.example.fireview.domain.dataserver.dto.DataServerProductResponse;
import com.example.fireview.domain.dataserver.dto.DataServerReview;
import com.example.fireview.domain.dataserver.client.DataServerClient;
import com.example.fireview.domain.dataserver.dto.response.CollectionStatus;
import com.example.fireview.domain.dataserver.dto.response.DataProductResponse;
import com.example.fireview.domain.product.entity.Product;
import com.example.fireview.domain.product.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DataProductServiceTest {

    private static final String PLATFORM = "kurly";
    private static final String PRODUCT_ID = "1000146248";

    @Mock DataServerClient dataServerClient;
    @Mock ProductRepository productRepository;
    @InjectMocks DataProductService service;

    private static DataServerProduct product() {
        return new DataServerProduct(PLATFORM, PRODUCT_ID, "샘플 상품", "https://kurly.com/p",
                "브랜드", "제조사", "판매자", 29900, "https://img", "식품 > 간편식", 128, 4.5,
                "2026-10-05T00:00:00+09:00");
    }

    private static DataServerProductResponse body(String status, DataServerProduct p,
                                                  List<DataServerReview> reviews, DataServerJob job) {
        return new DataServerProductResponse(status, p,
                reviews == null ? null : new DataServerProductResponse.Reviews(reviews, "next-1"), job);
    }

    private DataProductResponse call() {
        return service.getProduct(PLATFORM, PRODUCT_ID, null);
    }

    @Test
    void fresh면_상품과_리뷰를_내려준다() {
        when(dataServerClient.findProduct(any(), any())).thenReturn(Optional.of(
                body("fresh", product(),
                        List.of(new DataServerReview("r-1", "맛있어요", 5.0, "user**",
                                "2026-10-01T10:00:00+09:00", "옵션", List.of("img"), 3)), null)));

        DataProductResponse res = call();

        assertThat(res.collectionStatus()).isEqualTo(CollectionStatus.FRESH);
        assertThat(res.product().name()).isEqualTo("샘플 상품");
        assertThat(res.product().externalId()).isEqualTo("kurly-1000146248");
        assertThat(res.reviews().items()).hasSize(1);
        assertThat(res.reviews().nextCursor()).isEqualTo("next-1");
        assertThat(res.analysis()).isNull();
    }

    @Test
    void stale도_상품을_내려준다() {
        // 실패가 아니다. 최신을 기다리면 화면이 크롤링 속도에 묶인다
        when(dataServerClient.findProduct(any(), any())).thenReturn(Optional.of(
                body("stale", product(), List.of(), new DataServerJob(7L, PLATFORM, PRODUCT_ID,
                        "running", "pending", "pending", null))));

        DataProductResponse res = call();

        assertThat(res.collectionStatus()).isEqualTo(CollectionStatus.STALE);
        assertThat(res.product()).isNotNull();
        assertThat(res.job().id()).isEqualTo(7L);
    }

    @Test
    void queued면_상품이_없고_job만_있다() {
        when(dataServerClient.findProduct(any(), any())).thenReturn(Optional.of(
                body("queued", null, null, new DataServerJob(9L, PLATFORM, PRODUCT_ID,
                        "pending", "pending", "pending", null))));

        DataProductResponse res = call();

        assertThat(res.collectionStatus()).isEqualTo(CollectionStatus.QUEUED);
        assertThat(res.product()).isNull();
        assertThat(res.reviews().items()).isEmpty();
        assertThat(res.job().id()).isEqualTo(9L);
    }

    @Test
    void Data_서버에_못_닿으면_UNAVAILABLE이다() {
        // 404 로 바꾸면 "상품이 없다"는 뜻이 되어버린다. 둘은 프론트가 다르게 다뤄야 한다
        when(dataServerClient.findProduct(any(), any())).thenReturn(Optional.empty());

        DataProductResponse res = call();

        assertThat(res.collectionStatus()).isEqualTo(CollectionStatus.UNAVAILABLE);
        assertThat(res.product()).isNull();
        assertThat(res.reviews().items()).isEmpty();
    }

    @Test
    void 번호표가_있으면_함께_내려준다() {
        when(dataServerClient.findProduct(any(), any()))
                .thenReturn(Optional.of(body("fresh", product(), List.of(), null)));
        when(productRepository.findByDataPlatformAndDataProductId(PLATFORM, PRODUCT_ID))
                .thenReturn(Optional.of(Product.builder().id(42L).name("샘플 상품").build()));

        assertThat(call().springProductId()).isEqualTo(42L);
    }

    @Test
    void 번호표가_없으면_null이고_새로_만들지_않는다() {
        // 열어보기만 해도 행이 생기면 Data 서버 상품 수만큼 빈 행이 쌓인다
        when(dataServerClient.findProduct(any(), any()))
                .thenReturn(Optional.of(body("fresh", product(), List.of(), null)));
        when(productRepository.findByDataPlatformAndDataProductId(anyString(), anyString()))
                .thenReturn(Optional.empty());

        assertThat(call().springProductId()).isNull();
        org.mockito.Mockito.verify(productRepository, org.mockito.Mockito.never())
                .save(any(Product.class));
    }

    @Test
    void job_조회는_없으면_빈값이다() {
        when(dataServerClient.findJob(1L)).thenReturn(Optional.empty());
        when(dataServerClient.findJob(2L)).thenReturn(Optional.of(
                new DataServerJob(2L, PLATFORM, PRODUCT_ID, "partial", "succeeded", "failed", "타임아웃")));

        assertThat(service.getJob(1L)).isEmpty();
        assertThat(service.getJob(2L)).get()
                .extracting(DataProductResponse.CollectionJobStatus::reviewStatus).isEqualTo("failed");
    }
}

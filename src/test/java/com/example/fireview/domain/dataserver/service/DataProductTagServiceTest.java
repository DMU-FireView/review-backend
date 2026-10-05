package com.example.fireview.domain.dataserver.service;

import com.example.fireview.domain.dataserver.DataServerProductKey;
import com.example.fireview.domain.dataserver.client.DataServerClient;
import com.example.fireview.domain.dataserver.dto.DataServerJob;
import com.example.fireview.domain.dataserver.dto.DataServerProduct;
import com.example.fireview.domain.dataserver.dto.DataServerProductResponse;
import com.example.fireview.domain.product.entity.Product;
import com.example.fireview.global.exception.CustomException;
import com.example.fireview.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DataProductTagServiceTest {

    private static final DataServerProductKey KEY =
            new DataServerProductKey("kurly", "1000146248");

    @Mock DataServerClient dataServerClient;
    @Mock ProductTagRegistry registry;
    @InjectMocks DataProductTagService service;

    private static DataServerProductResponse usable() {
        DataServerProduct p = new DataServerProduct("kurly", "1000146248", "토리든 마스크팩",
                "https://kurly.com/p", null, null, null, 17000, null, "뷰티", 1318, null, null);
        return new DataServerProductResponse("fresh", p,
                new DataServerProductResponse.Reviews(List.of(), null), null);
    }

    @Test
    void 번호표가_이미_있으면_Data_서버를_부르지_않는다() {
        // 찜을 걸 때마다 외부 호출을 하면 느려진다
        Product tag = Product.builder().id(42L).name("토리든 마스크팩").build();
        when(registry.find(KEY)).thenReturn(Optional.of(tag));

        assertThat(service.resolveOrCreate(KEY)).isSameAs(tag);

        verify(dataServerClient, never()).findProduct(any());
        verify(registry, never()).resolveOrCreate(any(), anyString());
    }

    @Test
    void 번호표가_없으면_상품을_확인하고_만든다() {
        when(registry.find(KEY)).thenReturn(Optional.empty());
        when(dataServerClient.findProduct(KEY)).thenReturn(Optional.of(usable()));
        when(registry.resolveOrCreate(KEY, "토리든 마스크팩"))
                .thenReturn(Product.builder().id(42L).name("토리든 마스크팩").build());

        assertThat(service.resolveOrCreate(KEY).getId()).isEqualTo(42L);
    }

    @Test
    void 수집_전이면_번호표를_만들지_않는다() {
        // 실재하는 상품인지도 모른다. 여기서 만들면 없는 상품이 찜 목록에 남는다
        when(registry.find(KEY)).thenReturn(Optional.empty());
        when(dataServerClient.findProduct(KEY)).thenReturn(Optional.of(
                new DataServerProductResponse("queued", null, null,
                        new DataServerJob(9L, "kurly", "1000146248",
                                "pending", "pending", "pending", null))));

        assertThatThrownBy(() -> service.resolveOrCreate(KEY))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.PRODUCT_NOT_COLLECTED);

        verify(registry, never()).resolveOrCreate(any(), anyString());
    }

    @Test
    void Data_서버에_못_닿으면_만들지_않는다() {
        when(registry.find(KEY)).thenReturn(Optional.empty());
        when(dataServerClient.findProduct(KEY)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.resolveOrCreate(KEY))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.DATA_SERVER_UNAVAILABLE);

        verify(registry, never()).resolveOrCreate(any(), anyString());
    }
}

package com.example.fireview.domain.dataserver.service;

import com.example.fireview.domain.dataserver.DataServerProductKey;
import com.example.fireview.domain.dataserver.client.DataServerClient;
import com.example.fireview.domain.dataserver.dto.DataServerProductResponse;
import com.example.fireview.domain.product.entity.Product;
import com.example.fireview.global.exception.CustomException;
import com.example.fireview.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Data 서버 상품에 번호표를 붙여 Spring 기능(찜·장바구니)에 연결한다.
 *
 * <p>{@link ProductTagRegistry} 는 "행을 만든다"만 하고, 여기서 "만들어도 되는 상품인가"를
 * 판단한다. 수집도 되지 않은 상품에 번호표를 붙이면 존재하지 않는 상품이 찜 목록에 남는다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DataProductTagService {

    private final DataServerClient dataServerClient;
    private final ProductTagRegistry registry;

    /**
     * 번호표를 찾고, 없으면 Data 서버에서 상품을 확인한 뒤 만든다.
     *
     * <p>이미 번호표가 있으면 Data 서버를 부르지 않는다. 찜을 걸 때마다 외부 호출을 하면
     * 느려지고, 이미 한 번 확인한 상품을 다시 확인할 이유도 없다.
     *
     * @throws CustomException 수집 전({@code PRODUCT_NOT_COLLECTED})이거나
     *                         Data 서버에 닿지 못했을 때({@code DATA_SERVER_UNAVAILABLE})
     */
    @Transactional
    public Product resolveOrCreate(DataServerProductKey key) {
        Optional<Product> existing = registry.find(key);
        if (existing.isPresent()) {
            return existing.get();
        }

        DataServerProductResponse response = dataServerClient.findProduct(key)
                .orElseThrow(() -> {
                    log.warn("[ProductTag] Data 서버 조회 실패 - key={}", key.asExternalId());
                    return new CustomException(ErrorCode.DATA_SERVER_UNAVAILABLE);
                });

        if (!response.hasUsableData()) {
            // queued. 수집이 끝나야 상품명을 알 수 있고, 애초에 실재하는 상품인지도 모른다.
            // 여기서 행을 만들면 존재하지 않는 상품이 찜 목록에 남는다.
            log.info("[ProductTag] 아직 수집 전이라 번호표를 만들지 않는다 - key={}", key.asExternalId());
            throw new CustomException(ErrorCode.PRODUCT_NOT_COLLECTED);
        }

        return registry.resolveOrCreate(key, response.product().name());
    }
}

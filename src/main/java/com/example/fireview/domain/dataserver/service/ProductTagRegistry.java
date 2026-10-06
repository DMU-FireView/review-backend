package com.example.fireview.domain.dataserver.service;

import com.example.fireview.domain.dataserver.DataServerProductKey;
import com.example.fireview.domain.product.entity.Product;
import com.example.fireview.domain.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Data 서버 상품에 Spring 쪽 번호표를 붙인다.
 *
 * <p>찜·장바구니·조회이력은 {@code products.id}(Long)를 FK 로 물고 있고 Data 서버는
 * {@code (platform, product_id)} 로 상품을 가리킨다. 이 사이를 잇는 행을 만든다.
 *
 * <p><b>상품을 열어보는 것만으로는 만들지 않는다.</b> 그렇게 하면 Data 서버에 있는
 * 상품 수만큼 빈 행이 쌓인다. 찜처럼 Spring 쪽에 기록이 실제로 필요해질 때만 부른다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductTagRegistry {

    /**
     * 기존 더미 상품이 쓰는 ID 구간. 새 번호표가 여기에 떨어지면 비켜간다.
     * {@code DataInitializer} 의 fallback 번호가 {@code 900_000_000_000} 부터 시작한다.
     */
    private static final long DUMMY_ID_FLOOR = 900_000_000_000L;
    private static final long DUMMY_ID_CEIL = 900_001_000_000L;

    /** ID 비트 수. JS 안전 정수(2^53 - 1) 안에 들어가야 웹 프론트에서 값이 안 틀어진다 */
    private static final int ID_BITS = 52;

    private final ProductRepository productRepository;

    /** 이미 붙어 있는 번호표만 찾는다. 만들지 않는다 */
    @Transactional(readOnly = true)
    public java.util.Optional<Product> find(DataServerProductKey key) {
        return productRepository.findByDataPlatformAndDataProductId(key.platform(), key.productId());
    }

    /**
     * 번호표를 찾고, 없으면 만든다.
     *
     * @param name Data 서버에서 받은 상품명. 알림·목록에 쓸 최소 표시용으로만 저장한다.
     *             가격·평점·리뷰는 저장하지 않는다 — 그쪽 주인은 Data 서버다
     */
    @Transactional
    public Product resolveOrCreate(DataServerProductKey key, String name) {
        return productRepository
                .findByDataPlatformAndDataProductId(key.platform(), key.productId())
                .orElseGet(() -> create(key, name));
    }

    private Product create(DataServerProductKey key, String name) {
        Product tag = Product.builder()
                .id(allocateId(key))
                .name(name == null || name.isBlank() ? key.asExternalId() : name)
                // 화면 표기용. Data 서버 수집기 이름을 대문자로 옮긴다
                .platform(key.platform().toUpperCase())
                .dataPlatform(key.platform())
                .dataProductId(key.productId())
                // category·avgRti 는 비워둔다. Data 서버는 분석을 주지 않고,
                // 카테고리 문자열은 이 enum 으로 안전하게 옮길 수 없다
                .build();
        try {
            Product saved = productRepository.saveAndFlush(tag);
            log.info("[ProductTag] 번호표 생성 - key={}, id={}", key.asExternalId(), saved.getId());
            return saved;
        } catch (DataIntegrityViolationException e) {
            // 동시에 같은 상품을 찜한 요청이 둘 있었다. 유니크 제약이 하나만 통과시킨다.
            // 먼저 들어간 쪽을 다시 읽어 돌려준다.
            return productRepository
                    .findByDataPlatformAndDataProductId(key.platform(), key.productId())
                    .orElseThrow(() -> e);
        }
    }

    /**
     * 외부 식별자에서 ID 를 결정론적으로 뽑는다.
     *
     * <p>시퀀스를 쓰지 않는 이유: 같은 상품에 대한 동시 요청이 서로 다른 ID 로 두 행을
     * 만들려 하면 유니크 제약에 걸려 한쪽이 실패한다. 같은 키에서 늘 같은 ID 가 나오면
     * 재시도해도 같은 행을 가리키므로 그 경합이 사라진다. DB 시퀀스 생성 DDL 도 필요 없다.
     *
     * <p><b>상한은 2^52 이다.</b> 프론트가 Flutter 웹이라 JSON 숫자를 JS double 로 읽고,
     * 2^53 을 넘는 정수는 반올림된다. 처음에는 SHA-256 상위 63비트를 써서 10^18 대의 ID 가
     * 나왔는데, 그러면 브라우저가 받는 순간 값이 틀어져 그 ID 로 상세·찜을 부르면 엉뚱한
     * 번호가 간다. 상위 52비트만 쓴다. 상품 10만 개 기준 충돌 확률은 10^-6 수준이고,
     * 충돌하면 기본키 제약에 걸려 바로 드러난다.
     */
    static long allocateId(DataServerProductKey key) {
        byte[] digest = sha256(key.asExternalId());
        long value = 0;
        for (int i = 0; i < 8; i++) {
            value = (value << 8) | (digest[i] & 0xFFL);
        }
        value >>>= (64 - ID_BITS);   // 상위 52비트, 항상 양수
        if (value == 0) {
            value = 1;
        }
        if (value >= DUMMY_ID_FLOOR && value < DUMMY_ID_CEIL) {
            value += DUMMY_ID_CEIL - DUMMY_ID_FLOOR;
        }
        return value;
    }

    private static byte[] sha256(String text) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 을 쓸 수 없는 환경이다", e);
        }
    }
}

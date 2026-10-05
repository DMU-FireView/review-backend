package com.example.fireview.domain.dataserver;

import java.util.Optional;

/**
 * Data 서버의 상품 식별자.
 *
 * <p>Data 서버는 {@code (platform, product_id)} 복합키를 쓴다. 쇼핑몰이 8곳이라
 * 몰이 다르면 같은 {@code product_id} 가 겹칠 수 있어 단일 값으로는 상품을 특정할 수 없다.
 *
 * <p>Spring 안에서는 이 둘을 {@code "{platform}-{productId}"} 한 문자열로 들고 다닌다.
 * 웹훅 규격(docs/webhook-contract.md)이 이미 {@code "naver-7195971829"} 형태를 쓰고 있어
 * 그 표기를 그대로 따른다. 챗봇 세션이나 알림 링크처럼 식별자를 한 칸에 담아야 하는
 * 자리가 많아서, 두 칼럼으로 쪼개는 것보다 이 편이 번지지 않는다.
 *
 * @param platform  수집기 이름 (예: naver, kurly, elevenst)
 * @param productId 쇼핑몰의 상품 ID
 */
public record DataServerProductKey(String platform, String productId) {

    /**
     * {@code "{platform}-{productId}"} 를 가른다.
     *
     * <p>첫 하이픈에서만 자른다. 플랫폼 이름에는 하이픈이 없지만 상품 ID 에는 있을 수 있다.
     *
     * @return 형식이 맞지 않으면 {@link Optional#empty()}
     */
    public static Optional<DataServerProductKey> parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        int sep = raw.indexOf('-');
        if (sep <= 0 || sep == raw.length() - 1) {
            return Optional.empty();
        }
        String platform = raw.substring(0, sep).trim();
        String productId = raw.substring(sep + 1).trim();
        if (platform.isEmpty() || productId.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new DataServerProductKey(platform, productId));
    }

    /** 다시 한 문자열로 */
    public String asExternalId() {
        return platform + "-" + productId;
    }
}

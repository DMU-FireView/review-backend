package com.example.fireview.domain.product.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

/**
 * 플랫폼별 구매 링크 (상품에 Embedded Collection으로 포함)
 *
 * 지원 플랫폼: NAVER, COUPANG, 11ST, GMARKET, SSG, TMON
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlatformLink {

    @Column(name = "platform_name", length = 30)
    private String platform;   // 예: "NAVER", "COUPANG", "11ST"

    @Column(name = "platform_price")
    private Long price;        // 해당 플랫폼에서의 가격 (원)

    @Column(name = "platform_url", length = 1000)
    private String url;        // 구매 페이지 URL
}

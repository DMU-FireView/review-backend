package com.example.fireview.domain.product.dto;

import com.example.fireview.domain.product.entity.PlatformLink;

/**
 * 플랫폼별 구매 링크 응답 DTO
 */
public record PlatformLinkDto(
        String platform,   // 예: "NAVER", "COUPANG", "11ST"
        Long price,        // 해당 플랫폼 가격 (원)
        String url         // 구매 페이지 URL
) {
    public static PlatformLinkDto from(PlatformLink link) {
        return new PlatformLinkDto(link.getPlatform(), link.getPrice(), link.getUrl());
    }
}

package com.example.fireview.domain.product.dto;

import com.example.fireview.domain.product.entity.Category;
import com.example.fireview.domain.product.entity.Product;
import com.example.fireview.domain.review.entity.TrustGrade;

import java.util.List;

/**
 * 상품 응답 DTO
 *
 * [표시 정책]
 * - 상품 단위에는 RTI 수치(avgRti)와 등급(rtiGrade, rtiLevel)을 모두 표시합니다.
 * - rtiLevel: AI 서버와 동일한 "safe" | "warn" | "danger" 형식
 * - platforms: 멀티 플랫폼 구매 링크 + 가격 (교수님 반응 좋았던 기능)
 * - lowestPrice / lowestPlatform: 최저가 정보
 */
public record ProductResponse(
        Long id,
        String name,
        String imageUrl,
        Long price,
        Category category,
        String categoryDisplayName,
        String platform,
        Double avgRti,               // RTI 수치 (상품 단위에서만 노출)
        TrustGrade rtiGrade,         // SAFE | SUSPICIOUS | DANGER
        String rtiLevel,             // "safe" | "warn" | "danger" (AI 서버 형식 통일)
        String rtiColor,
        Integer reviewCount,
        Double avgRating,
        List<PlatformLinkDto> platforms,   // 멀티 플랫폼 구매 링크
        Long lowestPrice,                  // 최저가 (원)
        String lowestPlatform              // 최저가 플랫폼 이름
) {
    public static ProductResponse from(Product product) {
        TrustGrade grade = TrustGrade.fromScore(product.getAvgRti());
        List<PlatformLinkDto> platformDtos = product.getPlatformLinks().stream()
                .map(PlatformLinkDto::from)
                .toList();
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getImageUrl(),
                product.getPrice(),
                product.getCategory(),
                product.getCategory().getDisplayName(),
                product.getPlatform(),
                product.getAvgRti(),
                grade,
                grade.toLevel(),
                grade.getColor(),
                product.getReviewCount(),
                product.getAvgRating(),
                platformDtos,
                product.getLowestPrice(),
                product.getLowestPlatform()
        );
    }
}
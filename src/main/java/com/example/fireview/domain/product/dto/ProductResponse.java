package com.example.fireview.domain.product.dto;

import com.example.fireview.domain.product.client.NaverShoppingItem;
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
        String lowestPlatform,             // 최저가 플랫폼 이름
        String productUrl                  // 네이버 상품 페이지 URL (AI 분석 요청 시 사용)
) {
    /**
     * 네이버 쇼핑 검색 결과 아이템 → ProductResponse 변환.
     * RTI 데이터가 없으므로 기본값(50.0 / SUSPICIOUS)으로 채운다.
     */
    public static ProductResponse fromNaverItem(NaverShoppingItem item) {
        // 네이버 title에는 <b>태그가 포함되므로 제거
        String name = item.title().replaceAll("<[^>]*>", "").trim();

        long price = 0L;
        try { price = Long.parseLong(item.lprice()); } catch (NumberFormatException ignored) {}

        long productId = 0L;
        try { productId = Long.parseLong(item.productId()); } catch (NumberFormatException ignored) {}

        Category category = CategoryMapper.fromNaver(item.category1());
        TrustGrade grade = TrustGrade.SUSPICIOUS; // 아직 RTI 분석 전

        return new ProductResponse(
                productId,
                name,
                item.image(),
                price,
                category,
                category.getDisplayName(),
                item.mallName().isBlank() ? "NAVER" : item.mallName(),
                50.0,               // RTI 미분석 기본값
                grade,
                grade.toLevel(),
                grade.getColor(),
                0,                  // 리뷰 수 미집계
                0.0,                // 평점 미집계
                List.of(),
                price,
                item.mallName().isBlank() ? "NAVER" : item.mallName(),
                item.link()         // AI 분석 요청 시 productUrl로 사용
        );
    }

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
                product.getLowestPlatform(),
                null    // DB 상품은 platformLinks에 URL이 있으므로 별도 productUrl 불필요
        );
    }
}
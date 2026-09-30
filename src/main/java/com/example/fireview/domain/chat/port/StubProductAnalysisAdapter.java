package com.example.fireview.domain.chat.port;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Data 서버 연동 전까지 쓰는 임시 어댑터.
 *
 * 실제 구현(Data 서버 조회 API 호출)이 들어오면 이 빈을 제거한다.
 * app.chat.stub-analysis.enabled=false 로 두면 비활성화된다.
 *
 * 고정된 더미 컨텍스트를 돌려주므로 챗봇 흐름과 세이프가드는 지금도 검증할 수 있다.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.chat.stub-analysis.enabled", havingValue = "true", matchIfMissing = true)
public class StubProductAnalysisAdapter implements ProductAnalysisPort {

    @Override
    public Optional<ProductAnalysisContext> findContext(String productId) {
        if (productId == null || productId.isBlank()) {
            return Optional.empty();
        }
        log.debug("[Chat] 스텁 분석 컨텍스트 반환 - productId={}", productId);
        return Optional.of(new ProductAnalysisContext(
                productId,
                "샘플 상품 " + productId,
                29900,
                "패션 > 상의 > 니트",
                72.4,
                "주의",
                128,
                List.of("가격 대비 품질이 좋다", "배송이 빠르다"),
                List.of("사이즈가 작게 나온다", "보풀이 생긴다"),
                List.of("짧은 호평이 같은 날짜에 몰려 작성됨", "구매 인증되지 않은 리뷰 비율이 높음"),
                List.of(
                        new ProductAnalysisContext.SampleReview(
                                "가격 대비 만족스럽습니다. 다만 사이즈가 한 치수 작게 나와요.", 4, 81.2, "안전"),
                        new ProductAnalysisContext.SampleReview(
                                "배송 빨라요. 좋아요.", 5, 34.8, "위험"),
                        new ProductAnalysisContext.SampleReview(
                                "두 달 입었는데 보풀이 좀 생겼습니다. 그래도 이 가격이면 괜찮아요.", 3, 88.5, "안전")
                )
        ));
    }
}

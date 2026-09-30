package com.example.fireview.domain.chat.port;

import java.util.Optional;

/**
 * 챗봇이 상품 분석 컨텍스트를 얻는 통로.
 *
 * 확정 아키텍처에서 상품·리뷰·분석 결과는 Data 서버가 소유하므로,
 * 실제 구현은 Data 서버 조회 API 를 호출하는 어댑터가 된다.
 * Data 서버 연동 전까지는 스텁이 이 자리를 채운다.
 */
public interface ProductAnalysisPort {

    /**
     * 상품 분석 컨텍스트를 조회한다.
     *
     * @return 분석 결과가 없으면 Optional.empty() — 아직 분석되지 않은 상품일 수 있다
     */
    Optional<ProductAnalysisContext> findContext(String productId);
}

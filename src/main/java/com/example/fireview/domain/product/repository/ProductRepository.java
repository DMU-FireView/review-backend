package com.example.fireview.domain.product.repository;

import com.example.fireview.domain.product.entity.Category;
import com.example.fireview.domain.product.entity.Product;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByCategory(Category category);
    List<Product> findByCategoryIn(List<Category> categories);
    List<Product> findTop10ByOrderByAvgRtiDesc();
    List<Product> findByAvgRtiLessThanOrderByAvgRtiAsc(double threshold);
    // platformLinks를 JOIN FETCH로 한 번에 로딩 → LazyInitializationException 방지
    @Query("SELECT DISTINCT p FROM Product p LEFT JOIN FETCH p.platformLinks WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Product> findByNameContainingIgnoreCaseWithLinks(@Param("keyword") String keyword);

    @Query("SELECT p FROM Product p WHERE p.avgRti < :threshold ORDER BY p.avgRti ASC")
    List<Product> findRiskyProducts(double threshold);

    Optional<Product> findByNaverProductId(String naverProductId);

    /**
     * Data 서버 주소로 Spring 쪽 상품을 찾는다.
     *
     * <p>Data 서버는 {@code (platform, product_id)} 복합키를 쓰는데 찜·장바구니·조회이력은
     * Spring 의 Long id 를 FK 로 물고 있다. 이 조회가 그 사이를 잇는 번호표 역할을 한다.
     */
    Optional<Product> findByDataPlatformAndDataProductId(String dataPlatform, String dataProductId);

    /** 홈 목록 후보. Data 서버 상품만, 최근에 들어온 순. 이 중에서 분야를 섞어 고른다 */
    List<Product> findTop300ByDataPlatformIsNotNullOrderByCreatedAtDesc();

    /**
     * 챗봇 추천 후보. 같은 카테고리의 Data 서버 상품 중 가격이 범위 안인 것을 리뷰 많은 순으로.
     *
     * <p>번호표(data_platform, data_product_id)가 없거나 공백뿐인 행은 상세 화면을 열 수 없으므로
     * 뺀다({@link Product#hasDataServerAddress()} 와 같은 뜻). LIMIT 전에 빠져야 그 자리를 다른
     * 유효 후보가 채우므로 쿼리에서 거른다. SQL TRIM 은 스페이스만 지우므로 탭·개행만 있는 값은
     * 호출자가 한 번 더 거른다.
     * 가격이 없는 상품은 BETWEEN 에 걸리지 않아 자연히 빠진다.
     */
    @Query("""
            SELECT p FROM Product p
            WHERE p.category = :category
              AND p.id <> :excludeId
              AND p.dataPlatform IS NOT NULL AND TRIM(p.dataPlatform) <> ''
              AND p.dataProductId IS NOT NULL AND TRIM(p.dataProductId) <> ''
              AND p.price BETWEEN :minPrice AND :maxPrice
            ORDER BY p.reviewCount DESC NULLS LAST, p.id ASC
            """)
    List<Product> findRecommendationCandidatesInPriceRange(@Param("category") Category category,
                                                           @Param("excludeId") Long excludeId,
                                                           @Param("minPrice") long minPrice,
                                                           @Param("maxPrice") long maxPrice,
                                                           Limit limit);

    /** 기준 상품 가격을 모를 때의 추천 후보. 가격 조건만 빠지고 나머지는 위와 같다 */
    @Query("""
            SELECT p FROM Product p
            WHERE p.category = :category
              AND p.id <> :excludeId
              AND p.dataPlatform IS NOT NULL AND TRIM(p.dataPlatform) <> ''
              AND p.dataProductId IS NOT NULL AND TRIM(p.dataProductId) <> ''
            ORDER BY p.reviewCount DESC NULLS LAST, p.id ASC
            """)
    List<Product> findRecommendationCandidates(@Param("category") Category category,
                                               @Param("excludeId") Long excludeId,
                                               Limit limit);
}

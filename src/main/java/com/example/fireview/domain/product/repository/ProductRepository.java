package com.example.fireview.domain.product.repository;

import com.example.fireview.domain.product.entity.Category;
import com.example.fireview.domain.product.entity.Product;
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
}
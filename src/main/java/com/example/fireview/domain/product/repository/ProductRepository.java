package com.example.fireview.domain.product.repository;

import com.example.fireview.domain.product.entity.Category;
import com.example.fireview.domain.product.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

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
}
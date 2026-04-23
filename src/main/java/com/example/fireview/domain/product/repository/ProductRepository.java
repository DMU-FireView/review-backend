package com.example.fireview.domain.product.repository;

import com.example.fireview.domain.product.entity.Category;
import com.example.fireview.domain.product.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByCategory(Category category);
    List<Product> findByCategoryIn(List<Category> categories);
    List<Product> findTop10ByOrderByAvgRtiDesc();
    List<Product> findByAvgRtiLessThanOrderByAvgRtiAsc(double threshold);
    List<Product> findByNameContainingIgnoreCase(String keyword);

    @Query("SELECT p FROM Product p WHERE p.avgRti < :threshold ORDER BY p.avgRti ASC")
    List<Product> findRiskyProducts(double threshold);
}
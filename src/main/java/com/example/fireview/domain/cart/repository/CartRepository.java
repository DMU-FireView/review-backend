package com.example.fireview.domain.cart.repository;

import com.example.fireview.domain.cart.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface CartRepository extends JpaRepository<CartItem, Long> {

    List<CartItem> findByUser_IdOrderByCreatedAtDesc(Long userId);

    Optional<CartItem> findByUser_IdAndProduct_Id(Long userId, Long productId);

    boolean existsByUser_IdAndProduct_Id(Long userId, Long productId);

    void deleteByUser_IdAndProduct_Id(Long userId, Long productId);

    void deleteAllByUser_Id(Long userId);

    @Query("SELECT SUM(c.product.price * c.quantity) FROM CartItem c WHERE c.user.id = :userId")
    Long sumTotalByUserId(Long userId);
}

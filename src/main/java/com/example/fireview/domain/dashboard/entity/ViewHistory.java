package com.example.fireview.domain.dashboard.entity;

import com.example.fireview.domain.product.entity.Product;
import com.example.fireview.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "view_histories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ViewHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private LocalDateTime viewedAt;

    @PrePersist
    protected void onCreate() {
        viewedAt = LocalDateTime.now();
    }
}
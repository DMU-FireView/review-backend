package com.example.fireview.domain.dashboard.repository;

import com.example.fireview.domain.dashboard.entity.ViewHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ViewHistoryRepository extends JpaRepository<ViewHistory, Long> {
    List<ViewHistory> findTop10ByUser_IdOrderByViewedAtDesc(Long userId);
    boolean existsByUser_IdAndProduct_Id(Long userId, Long productId);
}
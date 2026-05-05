package com.example.fireview.domain.dashboard.repository;

import com.example.fireview.domain.dashboard.entity.SearchKeyword;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SearchKeywordRepository extends JpaRepository<SearchKeyword, Long> {
    Optional<SearchKeyword> findByKeyword(String keyword);
    List<SearchKeyword> findTop10ByOrderBySearchCountDesc();
}
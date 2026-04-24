package com.example.fireview.domain.onboarding.repository;

import com.example.fireview.domain.onboarding.entity.UserPreference;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserPreferenceRepository extends JpaRepository<UserPreference, Long> {
    Optional<UserPreference> findByUser_Id(Long userId);
    boolean existsByUser_Id(Long userId);
}
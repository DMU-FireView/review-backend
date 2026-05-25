package com.example.fireview.domain.user.repository;

import com.example.fireview.domain.user.entity.UserSetting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserSettingRepository extends JpaRepository<UserSetting, Long> {
    Optional<UserSetting> findByUser_Id(Long userId);
}

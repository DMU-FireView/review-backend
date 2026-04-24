package com.example.fireview.domain.user.service;

import com.example.fireview.domain.user.dto.UserResponse;
import com.example.fireview.domain.user.entity.User;
import com.example.fireview.domain.user.repository.UserRepository;
import com.example.fireview.global.exception.CustomException;
import com.example.fireview.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;

    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }

    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }

    public UserResponse getProfile(String email) {
        return UserResponse.from(findByEmail(email));
    }

    @Transactional
    public void markOnboardingComplete(Long userId) {
        User user = findById(userId);
        user.setOnboardingCompleted(true);
        userRepository.save(user);
    }
}
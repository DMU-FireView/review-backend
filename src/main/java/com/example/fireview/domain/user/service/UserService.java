package com.example.fireview.domain.user.service;

import com.example.fireview.domain.notification.repository.NotificationRepository;
import com.example.fireview.domain.report.repository.ReportRepository;
import com.example.fireview.domain.review.repository.ReviewFeedbackRepository;
import com.example.fireview.domain.user.dto.ProfileUpdateRequest;
import com.example.fireview.domain.user.dto.UserResponse;
import com.example.fireview.domain.user.dto.UserStatsResponse;
import com.example.fireview.domain.user.entity.User;
import com.example.fireview.domain.user.repository.UserRepository;
import com.example.fireview.domain.wishlist.repository.WishlistRepository;
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
    private final WishlistRepository wishlistRepository;
    private final ReviewFeedbackRepository feedbackRepository;
    private final ReportRepository reportRepository;
    private final NotificationRepository notificationRepository;

    // ── 조회 ─────────────────────────────────────────────────────────────────

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

    /** 이용 통계 조회 */
    public UserStatsResponse getStats(String email) {
        User user = findByEmail(email);
        long wishlistCount   = wishlistRepository.countByUser_Id(user.getId());
        long feedbackCount   = feedbackRepository.countByUser_Id(user.getId());
        long reportCount     = reportRepository.countByReporter_Id(user.getId());
        long unreadCount     = notificationRepository.countByReceiver_IdAndIsReadFalse(user.getId());
        return new UserStatsResponse(wishlistCount, feedbackCount, reportCount, unreadCount);
    }

    // ── 수정 ─────────────────────────────────────────────────────────────────

    /** 프로필 수정 (닉네임, 프로필 이미지) */
    @Transactional
    public UserResponse updateProfile(String email, ProfileUpdateRequest request) {
        User user = findByEmail(email);

        if (request.nickname() != null && !request.nickname().equals(user.getNickname())) {
            if (userRepository.existsByNickname(request.nickname())) {
                throw new CustomException(ErrorCode.NICKNAME_ALREADY_EXISTS);
            }
            user.setNickname(request.nickname());
        }

        if (request.profileImageUrl() != null) {
            user.setProfileImageUrl(request.profileImageUrl());
        }

        return UserResponse.from(userRepository.save(user));
    }

    /** 회원 탈퇴 */
    @Transactional
    public void deleteAccount(String email) {
        User user = findByEmail(email);
        userRepository.delete(user);
    }

    // ── 내부 사용 ────────────────────────────────────────────────────────────

    @Transactional
    public void markOnboardingComplete(Long userId) {
        User user = findById(userId);
        user.setOnboardingCompleted(true);
        userRepository.save(user);
    }
}

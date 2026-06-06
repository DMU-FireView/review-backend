package com.example.fireview.domain.user.service;

import com.example.fireview.domain.user.dto.UserSettingResponse;
import com.example.fireview.domain.user.dto.UserSettingUpdateRequest;
import com.example.fireview.domain.user.entity.User;
import com.example.fireview.domain.user.entity.UserSetting;
import com.example.fireview.domain.user.repository.UserSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserSettingService {

    private final UserSettingRepository userSettingRepository;
    private final UserService userService;

    public UserSettingResponse getSettings(String email) {
        User user = userService.findByEmail(email);
        return userSettingRepository.findByUser_Id(user.getId())
                .map(UserSettingResponse::from)
                .orElse(UserSettingResponse.defaults());
    }

    @Transactional
    public UserSettingResponse updateSettings(String email, UserSettingUpdateRequest req) {
        User user = userService.findByEmail(email);
        UserSetting s = userSettingRepository.findByUser_Id(user.getId())
                .orElseGet(() -> UserSetting.builder().user(user).build());

        if (req.notifyRiskyProduct() != null)         s.setNotifyRiskyProduct(req.notifyRiskyProduct());
        if (req.notifyAnalysisComplete() != null)     s.setNotifyAnalysisComplete(req.notifyAnalysisComplete());
        if (req.notifyFeedbackResult() != null)       s.setNotifyFeedbackResult(req.notifyFeedbackResult());
        if (req.notifyMarketing() != null)            s.setNotifyMarketing(req.notifyMarketing());
        if (req.rtiThreshold() != null)               s.setRtiThreshold(req.rtiThreshold());
        if (req.hideRiskyReviews() != null)           s.setHideRiskyReviews(req.hideRiskyReviews());
        if (req.showSuspiciousLabel() != null)        s.setShowSuspiciousLabel(req.showSuspiciousLabel());
        if (req.prioritizeVerifiedReviews() != null)  s.setPrioritizeVerifiedReviews(req.prioritizeVerifiedReviews());
        if (req.autoOpenAnalysisPopup() != null)      s.setAutoOpenAnalysisPopup(req.autoOpenAnalysisPopup());
        if (req.cardDensity() != null)                s.setCardDensity(req.cardDensity());
        if (req.reviewSortOrder() != null)            s.setReviewSortOrder(req.reviewSortOrder());
        if (req.rtiLabelStyle() != null)              s.setRtiLabelStyle(req.rtiLabelStyle());
        if (req.theme() != null)                      s.setTheme(req.theme());
        if (req.allowDataAnalysis() != null)          s.setAllowDataAnalysis(req.allowDataAnalysis());

        return UserSettingResponse.from(userSettingRepository.save(s));
    }
}

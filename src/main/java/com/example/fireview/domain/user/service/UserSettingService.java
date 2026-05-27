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
    public UserSettingResponse updateSettings(String email, UserSettingUpdateRequest request) {
        User user = userService.findByEmail(email);
        UserSetting setting = userSettingRepository.findByUser_Id(user.getId())
                .orElseGet(() -> UserSetting.builder().user(user).build());

        if (request.notifyReportResult() != null) {
            setting.setNotifyReportResult(request.notifyReportResult());
        }
        if (request.notifyFeedback() != null) {
            setting.setNotifyFeedback(request.notifyFeedback());
        }
        if (request.notifyMarketing() != null) {
            setting.setNotifyMarketing(request.notifyMarketing());
        }

        return UserSettingResponse.from(userSettingRepository.save(setting));
    }
}

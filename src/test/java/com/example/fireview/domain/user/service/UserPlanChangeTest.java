package com.example.fireview.domain.user.service;

import com.example.fireview.domain.user.dto.UserResponse;
import com.example.fireview.domain.user.entity.PlanTier;
import com.example.fireview.domain.user.entity.User;
import com.example.fireview.domain.user.repository.UserRepository;
import com.example.fireview.global.exception.CustomException;
import com.example.fireview.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserPlanChangeTest {

    private static final String EMAIL = "namjh3505@naver.com";

    @Mock UserRepository userRepository;
    @InjectMocks UserService userService;

    private User user() {
        return User.builder().id(4L).email(EMAIL).nickname("남정현").build();
    }

    @Test
    void 무료에서_플러스로_바꾼다() {
        User u = user();
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(u));

        UserResponse res = userService.changeMyPlan(EMAIL, PlanTier.PLUS);

        assertThat(res.planTier()).isEqualTo(PlanTier.PLUS);
        assertThat(u.getEffectivePlan()).isEqualTo(PlanTier.PLUS);
    }

    @Test
    void 결제_없이_바꾸므로_만료_시각은_없다() {
        User u = user();
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(u));

        userService.changeMyPlan(EMAIL, PlanTier.PRO);

        assertThat(u.getPlanExpiresAt()).isNull();
    }

    @Test
    void 무료로_내리면_남아있던_만료_시각도_지운다() {
        User u = user();
        u.changePlan(PlanTier.PRO, LocalDateTime.now().plusDays(30));
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(u));

        UserResponse res = userService.changeMyPlan(EMAIL, PlanTier.FREE);

        assertThat(res.planTier()).isEqualTo(PlanTier.FREE);
        assertThat(res.planExpiresAt()).isNull();
    }

    @Test
    void 같은_요금제로_다시_바꿔도_오류가_아니다() {
        // 화면에서 같은 버튼을 두 번 눌러도 사용자가 오류를 볼 이유가 없다
        User u = user();
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(u));

        userService.changeMyPlan(EMAIL, PlanTier.PLUS);
        UserResponse again = userService.changeMyPlan(EMAIL, PlanTier.PLUS);

        assertThat(again.planTier()).isEqualTo(PlanTier.PLUS);
    }

    @Test
    void 없는_사용자면_USER_NOT_FOUND() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.changeMyPlan(EMAIL, PlanTier.PRO))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.USER_NOT_FOUND);
    }
}

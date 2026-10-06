package com.example.fireview.domain.user.controller;

import com.example.fireview.domain.feedback.dto.response.UnifiedFeedbackResponse;
import com.example.fireview.domain.feedback.service.FeedbackStatusService;
import com.example.fireview.domain.user.dto.*;
import com.example.fireview.domain.user.service.UserService;
import com.example.fireview.domain.user.service.UserSettingService;
import com.example.fireview.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "마이페이지", description = "프로필·설정·이용통계·회원탈퇴")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserSettingService userSettingService;
    private final FeedbackStatusService feedbackStatusService;

    /** GET /api/users/me — 내 프로필 조회 */
    @GetMapping("/me")
    public ApiResponse<UserResponse> getMyProfile(@AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.success(userService.getProfile(jwt.getSubject()));
    }

    /** GET /api/users/me/stats — 이용 통계 */
    @GetMapping("/me/stats")
    public ApiResponse<UserStatsResponse> getMyStats(@AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.success(userService.getStats(jwt.getSubject()));
    }

    /** PATCH /api/users/me — 프로필 수정 (닉네임, 이미지, 전화번호, 관심 카테고리) */
    @PatchMapping("/me")
    public ApiResponse<UserResponse> updateMyProfile(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ProfileUpdateRequest request) {
        return ApiResponse.success("프로필이 수정되었습니다.",
                userService.updateProfile(jwt.getSubject(), request));
    }

    /** PATCH /api/users/me/plan — 내 요금제 변경 */
    @Operation(summary = "내 요금제 변경", description = """
            로그인한 사용자 본인의 챗봇 요금제를 바꾼다.

            ```json
            { "planTier": "PLUS" }
            ```

            값은 `FREE` / `PLUS` / `PRO`. 그 외의 값은 `400` 이다.

            **결제 없이 즉시 바뀐다.** 결제 연동 전까지 쓰는 임시 동작이다.
            만료 시각은 두지 않는다(`planExpiresAt` = null).

            요금제는 JWT 가 아니라 DB 값이라 **재로그인 없이 바로 반영**된다.
            변경 직후 `GET /api/chat/quota` 를 부르면 새 한도가 보인다.
            오늘 이미 쓴 횟수는 유지된다 — FREE 에서 5회 쓰고 PLUS 로 바꾸면 남은 횟수는 95회다.

            같은 요금제로 다시 바꿔도 오류가 아니다.

            응답은 변경된 사용자 정보(`GET /api/users/me` 와 같은 형태)다.
            """)
    @PatchMapping("/me/plan")
    public ApiResponse<UserResponse> changeMyPlan(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody MyPlanUpdateRequest request) {
        return ApiResponse.success("요금제가 변경되었습니다.",
                userService.changeMyPlan(jwt.getSubject(), request.planTier()));
    }

    /** DELETE /api/users/me — 회원 탈퇴 */
    @DeleteMapping("/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMyAccount(@AuthenticationPrincipal Jwt jwt) {
        userService.deleteAccount(jwt.getSubject());
    }

    /** GET /api/users/me/activities — 최근 활동 목록 */
    @GetMapping("/me/activities")
    public ApiResponse<List<UserActivityResponse>> getMyActivities(@AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.success(userService.getRecentActivities(jwt.getSubject()));
    }

    /** GET /api/users/me/security — 보안 상태 조회 */
    @GetMapping("/me/security")
    public ApiResponse<UserSecurityResponse> getMySecurityStatus(@AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.success(userService.getSecurityStatus(jwt.getSubject()));
    }

    /** GET /api/users/me/settings — 설정 조회 */
    @GetMapping("/me/settings")
    public ApiResponse<UserSettingResponse> getMySettings(@AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.success(userSettingService.getSettings(jwt.getSubject()));
    }

    /** PATCH /api/users/me/settings — 설정 변경 */
    @PatchMapping("/me/settings")
    public ApiResponse<UserSettingResponse> updateMySettings(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UserSettingUpdateRequest request) {
        return ApiResponse.success("설정이 변경되었습니다.",
                userSettingService.updateSettings(jwt.getSubject(), request));
    }

    /** GET /api/users/me/feedback — 내 신고 + 분석 피드백 통합 목록 */
    @GetMapping("/me/feedback")
    public ApiResponse<List<UnifiedFeedbackResponse>> getMyFeedbackHistory(
            @AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.success(
                feedbackStatusService.getUnifiedFeedbacks(jwt.getSubject()));
    }
}

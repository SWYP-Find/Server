package com.swyp.app.domain.user.controller;

import com.swyp.app.domain.user.dto.request.CreateOnboardingProfileRequest;
import com.swyp.app.domain.user.dto.request.UpdateTendencyScoreRequest;
import com.swyp.app.domain.user.dto.request.UpdateUserProfileRequest;
import com.swyp.app.domain.user.dto.request.UpdateUserSettingsRequest;
import com.swyp.app.domain.user.dto.response.BootstrapResponse;
import com.swyp.app.domain.user.dto.response.MyProfileResponse;
import com.swyp.app.domain.user.dto.response.OnboardingProfileResponse;
import com.swyp.app.domain.user.dto.response.TendencyScoreHistoryResponse;
import com.swyp.app.domain.user.dto.response.TendencyScoreResponse;
import com.swyp.app.domain.user.dto.response.UpdateResultResponse;
import com.swyp.app.domain.user.dto.response.UserProfileResponse;
import com.swyp.app.domain.user.dto.response.UserSettingsResponse;
import com.swyp.app.domain.user.service.UserService;
import com.swyp.app.global.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class UserController {

    private final UserService userService;

    @GetMapping("/onboarding/bootstrap")
    public ApiResponse<BootstrapResponse> getBootstrap() {
        return ApiResponse.onSuccess(userService.getBootstrap());
    }

    @PostMapping("/onboarding/profile")
    public ApiResponse<OnboardingProfileResponse> createOnboardingProfile(
            @Valid @RequestBody CreateOnboardingProfileRequest request
    ) {
        return ApiResponse.onSuccess(userService.createOnboardingProfile(request));
    }

    @GetMapping("/users/{userTag}")
    public ApiResponse<UserProfileResponse> getUserProfile(@PathVariable String userTag) {
        return ApiResponse.onSuccess(userService.getUserProfile(userTag));
    }

    @GetMapping("/me/profile")
    public ApiResponse<MyProfileResponse> getMyProfile() {
        return ApiResponse.onSuccess(userService.getMyProfile());
    }

    @PatchMapping("/me/profile")
    public ApiResponse<MyProfileResponse> updateMyProfile(
            @Valid @RequestBody UpdateUserProfileRequest request
    ) {
        return ApiResponse.onSuccess(userService.updateMyProfile(request));
    }

    @GetMapping("/me/settings")
    public ApiResponse<UserSettingsResponse> getMySettings() {
        return ApiResponse.onSuccess(userService.getMySettings());
    }

    @PatchMapping("/me/settings")
    public ApiResponse<UpdateResultResponse> updateMySettings(
            @Valid @RequestBody UpdateUserSettingsRequest request
    ) {
        return ApiResponse.onSuccess(userService.updateMySettings(request));
    }

    @PutMapping("/me/tendency-scores")
    public ApiResponse<TendencyScoreResponse> updateMyTendencyScores(
            @Valid @RequestBody UpdateTendencyScoreRequest request
    ) {
        return ApiResponse.onSuccess(userService.updateMyTendencyScores(request));
    }

    @GetMapping("/me/tendency-scores/history")
    public ApiResponse<TendencyScoreHistoryResponse> getMyTendencyScoreHistory(
            @RequestParam(required = false) Long cursor,
            @RequestParam(required = false) Integer size
    ) {
        return ApiResponse.onSuccess(userService.getMyTendencyScoreHistory(cursor, size));
    }
}

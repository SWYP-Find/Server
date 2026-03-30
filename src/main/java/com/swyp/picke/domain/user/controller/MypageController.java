package com.swyp.picke.domain.user.controller;

import com.swyp.picke.domain.user.dto.request.UpdateNotificationSettingsRequest;
import com.swyp.picke.domain.user.dto.request.UpdateUserProfileRequest;
import com.swyp.picke.domain.user.dto.response.BattleRecordListResponse;
import com.swyp.picke.domain.user.dto.response.ContentActivityListResponse;
import com.swyp.picke.domain.user.dto.response.MypageResponse;
import com.swyp.picke.domain.user.dto.response.MyProfileResponse;
import com.swyp.picke.domain.user.dto.response.NotificationSettingsResponse;
import com.swyp.picke.domain.user.dto.response.RecapResponse;
import com.swyp.picke.domain.user.enums.ActivityType;
import com.swyp.picke.domain.user.enums.VoteSide;

import com.swyp.picke.domain.user.service.MypageService;
import com.swyp.picke.domain.user.service.UserService;
import com.swyp.picke.global.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/me")
public class MypageController {

    private final UserService userService;
    private final MypageService mypageService;

    @PatchMapping("/profile")
    public ApiResponse<MyProfileResponse> updateMyProfile(
            @Valid @RequestBody UpdateUserProfileRequest request
    ) {
        return ApiResponse.onSuccess(userService.updateMyProfile(request));
    }

    @GetMapping("/mypage")
    public ApiResponse<MypageResponse> getMypage() {
        return ApiResponse.onSuccess(mypageService.getMypage());
    }

    @GetMapping("/recap")
    public ApiResponse<RecapResponse> getRecap() {
        return ApiResponse.onSuccess(mypageService.getRecap());
    }

    @GetMapping("/battle-records")
    public ApiResponse<BattleRecordListResponse> getBattleRecords(
            @RequestParam(required = false) Integer offset,
            @RequestParam(required = false) Integer size,
            @RequestParam(name = "vote_side", required = false) VoteSide voteSide
    ) {
        return ApiResponse.onSuccess(mypageService.getBattleRecords(offset, size, voteSide));
    }

    @GetMapping("/content-activities")
    public ApiResponse<ContentActivityListResponse> getContentActivities(
            @RequestParam(required = false) Integer offset,
            @RequestParam(required = false) Integer size,
            @RequestParam(name = "activity_type", required = false) ActivityType activityType
    ) {
        return ApiResponse.onSuccess(mypageService.getContentActivities(offset, size, activityType));
    }

    @GetMapping("/notification-settings")
    public ApiResponse<NotificationSettingsResponse> getNotificationSettings() {
        return ApiResponse.onSuccess(mypageService.getNotificationSettings());
    }

    @PatchMapping("/notification-settings")
    public ApiResponse<NotificationSettingsResponse> updateNotificationSettings(
            @RequestBody UpdateNotificationSettingsRequest request
    ) {
        return ApiResponse.onSuccess(mypageService.updateNotificationSettings(request));
    }

}

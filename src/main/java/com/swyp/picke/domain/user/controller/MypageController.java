package com.swyp.picke.domain.user.controller;

import com.swyp.picke.domain.user.dto.request.UpdateNotificationSettingsRequest;
import com.swyp.picke.domain.user.dto.request.UpdateUserProfileRequest;
import com.swyp.picke.domain.user.dto.response.BattleRecordListResponse;
import com.swyp.picke.domain.user.dto.response.ContentActivityListResponse;
import com.swyp.picke.domain.user.dto.response.CreditHistoryListResponse;
import com.swyp.picke.domain.user.dto.response.MypageResponse;
import com.swyp.picke.domain.user.dto.response.MyProfileResponse;
import com.swyp.picke.domain.user.dto.response.NotificationSettingsResponse;
import com.swyp.picke.domain.user.dto.response.RecapResponse;
import com.swyp.picke.domain.user.enums.ActivityType;
import com.swyp.picke.domain.user.enums.VoteSide;

import com.swyp.picke.domain.user.service.MypageService;
import com.swyp.picke.domain.user.service.UserService;
import com.swyp.picke.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "마이페이지 API", description = "마이페이지, 활동 내역, 알림 설정 조회 및 수정")
public class MypageController {

    private final UserService userService;
    private final MypageService mypageService;

    @Operation(summary = "내 프로필 수정", description = "사용자의 프로필 정보를 수정합니다.")
    @PatchMapping("/profile")
    public ApiResponse<MyProfileResponse> updateMyProfile(
            @Valid @RequestBody UpdateUserProfileRequest request
    ) {
        return ApiResponse.onSuccess(userService.updateMyProfile(request));
    }

    @Operation(summary = "마이페이지 조회", description = "사용자의 마이페이지 정보를 조회합니다.")
    @GetMapping("/mypage")
    public ApiResponse<MypageResponse> getMypage() {
        return ApiResponse.onSuccess(mypageService.getMypage());
    }

    @Operation(summary = "내 리캡 조회", description = "사용자의 리캡 정보를 조회합니다.")
    @GetMapping("/recap")
    public ApiResponse<RecapResponse> getRecap() {
        return ApiResponse.onSuccess(mypageService.getRecap());
    }

    @Operation(summary = "내 배틀 참여 기록 조회", description = "사용자의 배틀 참여 기록을 조회합니다.")
    @GetMapping("/battle-records")
    public ApiResponse<BattleRecordListResponse> getBattleRecords(
            @RequestParam(required = false) Integer offset,
            @RequestParam(required = false) Integer size,
            @RequestParam(name = "vote_side", required = false) VoteSide voteSide
    ) {
        return ApiResponse.onSuccess(mypageService.getBattleRecords(offset, size, voteSide));
    }

    @Operation(summary = "내 콘텐츠 활동 내역 조회", description = "사용자의 콘텐츠 활동 내역을 조회합니다.")
    @GetMapping("/content-activities")
    public ApiResponse<ContentActivityListResponse> getContentActivities(
            @RequestParam(required = false) Integer offset,
            @RequestParam(required = false) Integer size,
            @RequestParam(name = "activity_type", required = false) ActivityType activityType
    ) {
        return ApiResponse.onSuccess(mypageService.getContentActivities(offset, size, activityType));
    }

    @Operation(summary = "크레딧 히스토리 조회", description = "사용자의 크레딧 지급 및 사용 내역을 조회합니다.")
    @GetMapping("/credits/history")
    public ApiResponse<CreditHistoryListResponse> getCreditHistory(
            @RequestParam(required = false) Integer offset,
            @RequestParam(required = false) Integer size
    ) {
        return ApiResponse.onSuccess(mypageService.getCreditHistory(offset, size));
    }

    @Operation(summary = "알림 설정 조회", description = "사용자의 알림 설정 정보를 조회합니다.")
    @GetMapping("/notification-settings")
    public ApiResponse<NotificationSettingsResponse> getNotificationSettings() {
        return ApiResponse.onSuccess(mypageService.getNotificationSettings());
    }

    @Operation(summary = "알림 설정 수정", description = "사용자의 알림 설정 정보를 수정합니다.")
    @PatchMapping("/notification-settings")
    public ApiResponse<NotificationSettingsResponse> updateNotificationSettings(
            @RequestBody UpdateNotificationSettingsRequest request
    ) {
        return ApiResponse.onSuccess(mypageService.updateNotificationSettings(request));
    }

}

package com.swyp.picke.domain.battle.controller;

import com.swyp.picke.domain.battle.dto.response.BattleListResponse;
import com.swyp.picke.domain.battle.dto.response.BattleUserDetailResponse;
import com.swyp.picke.domain.battle.dto.response.TodayBattleListResponse;
import com.swyp.picke.domain.battle.service.BattleService;
import com.swyp.picke.domain.user.dto.response.UserBattleStatusResponse;
import com.swyp.picke.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "배틀 API", description = "배틀 조회")
@RestController
@RequestMapping("/api/v1/battles")
@RequiredArgsConstructor
public class BattleController {

    private final BattleService battleService;

    @Operation(summary = "오늘의 배틀 목록 조회 (최대 3개)")
    @GetMapping("/today")
    public ApiResponse<TodayBattleListResponse> getTodayBattles() {
        return ApiResponse.onSuccess(battleService.getTodayBattles());
    }

    @Operation(summary = "배틀 목록 조회")
    @GetMapping
    public ApiResponse<BattleListResponse> getBattles(
            @Parameter(description = "페이지 번호 (1부터 시작)", example = "1")
            @RequestParam(value = "page", defaultValue = "1") int page,
            @Parameter(description = "페이지 크기", example = "10")
            @RequestParam(value = "size", defaultValue = "10") int size,
            @Parameter(description = "콘텐츠 상태 (ALL, PENDING, PUBLISHED, REJECTED, ARCHIVED)", example = "ALL")
            @RequestParam(value = "status", required = false, defaultValue = "ALL") String status
    ) {
        return ApiResponse.onSuccess(battleService.getBattles(page, size, status));
    }

    @Operation(summary = "배틀 상세 조회")
    @GetMapping("/{battleId}")
    public ApiResponse<BattleUserDetailResponse> getBattleDetail(@PathVariable Long battleId) {
        return ApiResponse.onSuccess(battleService.getBattleDetail(battleId));
    }

    @Operation(summary = "사용자 배틀 진행 상태 조회")
    @GetMapping("/{battleId}/status")
    public ApiResponse<UserBattleStatusResponse> getUserBattleStatus(@PathVariable Long battleId) {
        return ApiResponse.onSuccess(battleService.getUserBattleStatus(battleId));
    }
}

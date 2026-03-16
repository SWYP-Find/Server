package com.swyp.app.domain.battle.controller;

import com.swyp.app.domain.battle.dto.response.BattleUserDetailResponse;
import com.swyp.app.domain.battle.dto.response.TodayBattleListResponse;
import com.swyp.app.domain.battle.service.BattleService;
import com.swyp.app.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "배틀 API (사용자)", description = "배틀 조회")
@RestController
@RequestMapping("/api/v1/battles")
@RequiredArgsConstructor
public class BattleController {

    private final BattleService battleService;

    @Operation(summary = "오늘의 배틀 목록 조회 (스와이프 UI용, 최대 5개)")
    @GetMapping("/today")
    public ApiResponse<TodayBattleListResponse> getTodayBattles() {
        return ApiResponse.onSuccess(battleService.getTodayBattles());
    }

    @Operation(summary = "배틀 상세 조회")
    @GetMapping("/{battleId}")
    public ApiResponse<BattleUserDetailResponse> getBattleDetail(
            @PathVariable UUID battleId
    ) {
        return ApiResponse.onSuccess(battleService.getBattleDetail(battleId));
    }
}
package com.swyp.picke.domain.battle.controller;

import com.swyp.picke.domain.battle.dto.response.BattleListResponse;
import com.swyp.picke.domain.battle.dto.response.BattleUserDetailResponse;
import com.swyp.picke.domain.battle.dto.response.TodayBattleListResponse;
import com.swyp.picke.domain.battle.service.BattleService;
import com.swyp.picke.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

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

    @Operation(summary = "배틀 전체 목록 조회", description = "페이징 및 타입별(ALL, BATTLE, QUIZ, VOTE) 필터링된 배틀 목록을 조회합니다.")
    @GetMapping
    public ApiResponse<BattleListResponse> getBattles(
            @Parameter(description = "페이지 번호 (1부터 시작)", example = "1")
            @RequestParam(value = "page", defaultValue = "1") int page,
            @Parameter(description = "페이지 크기", example = "10")
            @RequestParam(value = "size", defaultValue = "10") int size,
            @Parameter(description = "콘텐츠 타입 (ALL, BATTLE, QUIZ, VOTE)", example = "ALL")
            @RequestParam(value = "type", required = false, defaultValue = "ALL") String type
    ) {
        return ApiResponse.onSuccess(battleService.getBattles(page, size, type));
    }

    @Operation(summary = "배틀 상세 조회")
    @GetMapping("/{battleId}")
    public ApiResponse<BattleUserDetailResponse> getBattleDetail(
            @PathVariable Long battleId
    ) {
        return ApiResponse.onSuccess(battleService.getBattleDetail(battleId));
    }
}
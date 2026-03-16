package com.swyp.app.domain.battle.controller;

import com.swyp.app.domain.battle.dto.request.AdminBattleCreateRequest;
import com.swyp.app.domain.battle.dto.request.AdminBattleUpdateRequest;
import com.swyp.app.domain.battle.dto.response.AdminBattleDeleteResponse;
import com.swyp.app.domain.battle.dto.response.AdminBattleDetailResponse;
import com.swyp.app.domain.battle.service.BattleService;
import com.swyp.app.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "배틀 API (관리자)", description = "배틀 생성/수정/삭제 (관리자 전용)")
@RestController
@RequestMapping("/api/v1/admin/battles")
@RequiredArgsConstructor
public class AdminBattleController {

    private final BattleService battleService;

    @Operation(summary = "배틀 생성")
    @PostMapping
    public ApiResponse<AdminBattleDetailResponse> createBattle(
            @RequestBody @Valid AdminBattleCreateRequest request,
            @AuthenticationPrincipal Long adminUserId
    ) {
        // TODO: 로그인 기능 구현 후 @AuthenticationPrincipal adminUserId로 변경 예정
        // 현재 인증 정보가 없어 null이 들어오므로 테스트용 가짜 ID(1L)를 사용함
        Long testAdminId = (adminUserId != null) ? adminUserId : 1L;

        return ApiResponse.onSuccess(battleService.createBattle(request, testAdminId));
    }

    @Operation(summary = "배틀 수정 (변경 필드만 포함)")
    @PatchMapping("/{battleId}")
    public ApiResponse<AdminBattleDetailResponse> updateBattle(
            @PathVariable UUID battleId,
            @RequestBody @Valid AdminBattleUpdateRequest request
    ) {
        return ApiResponse.onSuccess(battleService.updateBattle(battleId, request));
    }

    @Operation(summary = "배틀 삭제")
    @DeleteMapping("/{battleId}")
    public ApiResponse<AdminBattleDeleteResponse> deleteBattle(
            @PathVariable UUID battleId
    ) {
        return ApiResponse.onSuccess(battleService.deleteBattle(battleId));
    }
}
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "배틀 API (관리자)", description = "배틀 생성/수정/삭제 (관리자 전용)")
@RestController
@RequestMapping("/api/v1/admin/battles")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminBattleController {

    private final BattleService battleService;

    @Operation(summary = "배틀 생성")
    @PostMapping
    public ApiResponse<AdminBattleDetailResponse> createBattle(
            @RequestBody @Valid AdminBattleCreateRequest request,
            @AuthenticationPrincipal Long adminUserId
    ) {
        return ApiResponse.onSuccess(battleService.createBattle(request, adminUserId));
    }

    @Operation(summary = "배틀 수정 (변경 필드만 포함)")
    @PatchMapping("/{battleId}")
    public ApiResponse<AdminBattleDetailResponse> updateBattle(
            @PathVariable Long battleId,
            @RequestBody @Valid AdminBattleUpdateRequest request
    ) {
        return ApiResponse.onSuccess(battleService.updateBattle(battleId, request));
    }

    @Operation(summary = "배틀 삭제")
    @DeleteMapping("/{battleId}")
    public ApiResponse<AdminBattleDeleteResponse> deleteBattle(
            @PathVariable Long battleId
    ) {
        return ApiResponse.onSuccess(battleService.deleteBattle(battleId));
    }
}
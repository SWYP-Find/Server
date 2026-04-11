package com.swyp.picke.domain.admin.controller;

import com.swyp.picke.domain.admin.dto.battle.request.AdminBattleCreateRequest;
import com.swyp.picke.domain.admin.dto.battle.response.AdminBattleDeleteResponse;
import com.swyp.picke.domain.admin.dto.battle.response.AdminBattleDetailResponse;
import com.swyp.picke.domain.admin.dto.battle.request.AdminBattleUpdateRequest;
import com.swyp.picke.domain.admin.service.AdminBattleService;
import com.swyp.picke.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "관리자 배틀 API", description = "관리자 배틀 콘텐츠 생성, 조회, 수정, 삭제")
@RestController
@RequestMapping("/api/v1/admin/battles")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminBattleController {

    private final AdminBattleService adminBattleService;

    @Operation(summary = "배틀 생성")
    @PostMapping
    public ApiResponse<AdminBattleDetailResponse> createBattle(
            @RequestBody @Valid AdminBattleCreateRequest request,
            @AuthenticationPrincipal Long adminUserId
    ) {
        return ApiResponse.onSuccess(adminBattleService.createBattle(request, adminUserId));
    }

    @Operation(summary = "배틀 상세 조회")
    @GetMapping("/{battleId}")
    public ApiResponse<AdminBattleDetailResponse> getBattleDetail(@PathVariable Long battleId) {
        return ApiResponse.onSuccess(adminBattleService.getBattleDetail(battleId));
    }

    @Operation(summary = "배틀 수정")
    @PatchMapping("/{battleId}")
    public ApiResponse<AdminBattleDetailResponse> updateBattle(
            @PathVariable Long battleId,
            @RequestBody @Valid AdminBattleUpdateRequest request
    ) {
        return ApiResponse.onSuccess(adminBattleService.updateBattle(battleId, request));
    }

    @Operation(summary = "배틀 삭제")
    @DeleteMapping("/{battleId}")
    public ApiResponse<AdminBattleDeleteResponse> deleteBattle(
            @PathVariable Long battleId
    ) {
        return ApiResponse.onSuccess(adminBattleService.deleteBattle(battleId));
    }

    @Operation(summary = "배틀 목록 조회")
    @GetMapping
    public ApiResponse<?> getBattles(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "status", required = false) String status
    ) {
        return ApiResponse.onSuccess(adminBattleService.getBattles(page, size, status));
    }
}
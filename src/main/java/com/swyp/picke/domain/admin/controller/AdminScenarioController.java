package com.swyp.picke.domain.admin.controller;

import com.swyp.picke.domain.admin.dto.scenario.request.AdminScenarioCreateRequest;
import com.swyp.picke.domain.admin.dto.scenario.request.AdminScenarioStatusUpdateRequest;
import com.swyp.picke.domain.admin.dto.scenario.response.AdminDeleteResponse;
import com.swyp.picke.domain.admin.dto.scenario.response.AdminScenarioCreateResponse;
import com.swyp.picke.domain.admin.dto.scenario.response.AdminScenarioDetailResponse;
import com.swyp.picke.domain.admin.dto.scenario.response.AdminScenarioResponse;
import com.swyp.picke.domain.admin.service.AdminScenarioService;
import com.swyp.picke.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "관리자 시나리오 API", description = "관리자 시나리오 생성, 조회, 수정, 삭제")
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminScenarioController {

    private final AdminScenarioService adminScenarioService;

    @Operation(summary = "배틀 시나리오 상세 조회")
    @GetMapping("/battles/{battleId}/scenario")
    public ApiResponse<AdminScenarioDetailResponse> getAdminBattleScenario(@PathVariable Long battleId) {
        return ApiResponse.onSuccess(adminScenarioService.getScenarioForAdmin(battleId));
    }

    @Operation(summary = "시나리오 생성")
    @PostMapping("/scenarios")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AdminScenarioCreateResponse> createScenario(@RequestBody AdminScenarioCreateRequest request) {
        return ApiResponse.onSuccess(adminScenarioService.createScenario(request));
    }

    @Operation(summary = "시나리오 본문 수정")
    @PutMapping("/scenarios/{scenarioId}")
    public ApiResponse<Void> updateScenarioContent(
            @PathVariable Long scenarioId,
            @RequestBody AdminScenarioCreateRequest request
    ) {
        adminScenarioService.updateScenarioContent(scenarioId, request);
        return ApiResponse.onSuccess(null);
    }

    @Operation(summary = "시나리오 상태 수정")
    @PatchMapping("/scenarios/{scenarioId}")
    public ApiResponse<AdminScenarioResponse> updateScenarioStatus(
            @PathVariable Long scenarioId,
            @RequestBody AdminScenarioStatusUpdateRequest request
    ) {
        return ApiResponse.onSuccess(adminScenarioService.updateScenarioStatus(scenarioId, request.status()));
    }

    @Operation(summary = "시나리오 삭제")
    @DeleteMapping("/scenarios/{scenarioId}")
    public ApiResponse<AdminDeleteResponse> deleteScenario(@PathVariable Long scenarioId) {
        return ApiResponse.onSuccess(adminScenarioService.deleteScenario(scenarioId));
    }
}
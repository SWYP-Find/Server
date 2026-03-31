package com.swyp.picke.domain.scenario.controller;

import com.swyp.picke.domain.battle.service.BattleService;
import com.swyp.picke.domain.scenario.dto.request.ScenarioCreateRequest;
import com.swyp.picke.domain.scenario.dto.request.ScenarioStatusUpdateRequest;
import com.swyp.picke.domain.scenario.dto.response.AdminDeleteResponse;
import com.swyp.picke.domain.scenario.dto.response.AdminScenarioDetailResponse;
import com.swyp.picke.domain.scenario.dto.response.AdminScenarioResponse;
import com.swyp.picke.domain.scenario.dto.response.UserScenarioResponse;
import com.swyp.picke.domain.scenario.service.ScenarioService;
import com.swyp.picke.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "시나리오 (Scenario)", description = "시나리오 API")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ScenarioController {

    private final ScenarioService scenarioService;
    private final BattleService battleService;

    @Operation(summary = "시나리오 통합 조회")
    @GetMapping("/battles/{battleId}/scenario")
    public ApiResponse<UserScenarioResponse> getBattleScenario(
            @PathVariable Long battleId,
            @RequestAttribute(value = "userId", required = false) Long userId
    ) {
        // 1. 배틀 데이터 조회 (제목, 철학자 리스트)
        var battleInfo = battleService.getBattleScenario(battleId);

        // 2. 시나리오 데이터 조회 (노드, 대사, 오디오 등)
        var scenarioInfo = scenarioService.getScenarioForUser(battleId, userId);

        // 3. UserScenarioResponse 최상단에 바로 값 세팅
        UserScenarioResponse response = scenarioInfo.toBuilder()
                .title(battleInfo.title())
                .philosophers(battleInfo.philosophers())
                .build();

        return ApiResponse.onSuccess(response);
    }

    @Operation(summary = "관리자용 배틀 시나리오 조회 (수정용)")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/battles/{battleId}/scenario")
    public ApiResponse<AdminScenarioDetailResponse> getAdminBattleScenario(
                                                                            @PathVariable Long battleId) {
        return ApiResponse.onSuccess(scenarioService.getScenarioForAdmin(battleId));
    }

    @Operation(summary = "시나리오 생성")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/scenarios")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Map<String, Object>> createScenario(
            @RequestBody ScenarioCreateRequest request) {

        Long scenarioId = scenarioService.createScenario(request);

        // Map.of 대신 null에도 안전한 HashMap 사용
        Map<String, Object> response = new java.util.HashMap<>();
        response.put("scenarioId", scenarioId);

        // 고정값 대신 프론트에서 보낸 상태값(PENDING 등)을 그대로 반환!
        response.put("status", request.status());

        return ApiResponse.onSuccess(response);
    }

    @Operation(summary = "시나리오 내용 수정")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/admin/scenarios/{scenarioId}")
    public ApiResponse<Void> updateScenarioContent(
            @PathVariable Long scenarioId,
            @RequestBody ScenarioCreateRequest request) {

        scenarioService.updateScenarioContent(scenarioId, request);
        return ApiResponse.onSuccess(null);
    }

    @Operation(summary = "시나리오 상태 수정 (PUBLISHED 변경 시 자동 오디오 처리)")
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/admin/scenarios/{scenarioId}")
    public ApiResponse<AdminScenarioResponse> updateScenarioStatus(
            @PathVariable Long scenarioId,
            @RequestBody ScenarioStatusUpdateRequest request) {

        return ApiResponse.onSuccess(scenarioService.updateScenarioStatus(scenarioId, request.status()));
    }

    @Operation(summary = "시나리오 삭제 (Soft Delete)")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/admin/scenarios/{scenarioId}")
    public ApiResponse<AdminDeleteResponse> deleteScenario(
            @PathVariable Long scenarioId) {

        return ApiResponse.onSuccess(scenarioService.deleteScenario(scenarioId));
    }
}
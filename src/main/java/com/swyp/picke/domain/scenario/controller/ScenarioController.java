package com.swyp.picke.domain.scenario.controller;

import com.swyp.picke.domain.battle.service.BattleService;
import com.swyp.picke.domain.scenario.dto.response.UserScenarioResponse;
import com.swyp.picke.domain.scenario.service.ScenarioService;
import com.swyp.picke.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "시나리오 API", description = "사용자 시나리오 조회")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ScenarioController {

    private final ScenarioService scenarioService;
    private final BattleService battleService;

    @Operation(summary = "배틀 시나리오 조회")
    @GetMapping("/battles/{battleId}/scenario")
    public ApiResponse<UserScenarioResponse> getBattleScenario(
            @PathVariable Long battleId,
            @RequestAttribute(value = "userId", required = false) Long userId
    ) {
        var battleInfo = battleService.getBattleScenario(battleId);
        var scenarioInfo = scenarioService.getScenarioForUser(battleId, userId);

        UserScenarioResponse response = scenarioInfo.toBuilder()
                .title(battleInfo.title())
                .philosophers(battleInfo.philosophers())
                .build();

        return ApiResponse.onSuccess(response);
    }
}

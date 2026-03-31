package com.swyp.picke.domain.scenario.dto.response;

import com.swyp.picke.domain.battle.dto.response.BattleScenarioResponse.PhilosopherProfileResponse;
import com.swyp.picke.domain.scenario.enums.AudioPathType;
import lombok.Builder;
import java.util.List;
import java.util.Map;

@Builder(toBuilder = true)
public record UserScenarioResponse(
        Long battleId,
        String title,
        List<PhilosopherProfileResponse> philosophers,
        Boolean isInteractive,
        Long startNodeId,
        AudioPathType recommendedPathKey,
        Map<AudioPathType, String> audios,
        List<NodeResponse> nodes
) {}
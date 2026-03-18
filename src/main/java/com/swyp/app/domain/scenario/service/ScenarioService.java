package com.swyp.app.domain.scenario.service;

import com.swyp.app.domain.scenario.dto.request.ScenarioCreateRequest;
import com.swyp.app.domain.scenario.dto.response.AdminDeleteResponse;
import com.swyp.app.domain.scenario.dto.response.AdminScenarioResponse;
import com.swyp.app.domain.scenario.dto.response.UserScenarioResponse;
import com.swyp.app.domain.scenario.enums.ScenarioStatus;

import java.util.UUID;

/**
 * 시나리오 관리를 위한 기본 CRUD 서비스 인터페이스
 */
public interface ScenarioService {
    UserScenarioResponse getScenarioForUser(UUID battleId, Long userId);
    UUID createScenario(ScenarioCreateRequest request);
    void updateScenarioContent(UUID scenarioId, ScenarioCreateRequest request);
    AdminScenarioResponse updateScenarioStatus(UUID scenarioId, ScenarioStatus status);
    AdminDeleteResponse deleteScenario(UUID scenarioId);
}
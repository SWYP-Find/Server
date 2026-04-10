package com.swyp.picke.domain.scenario.service;

import com.swyp.picke.domain.admin.dto.scenario.response.AdminDeleteResponse;
import com.swyp.picke.domain.admin.dto.scenario.response.AdminScenarioDetailResponse;
import com.swyp.picke.domain.admin.dto.scenario.response.AdminScenarioResponse;
import com.swyp.picke.domain.scenario.dto.request.ScenarioCreateRequest;
import com.swyp.picke.domain.scenario.dto.response.UserScenarioResponse;
import com.swyp.picke.domain.scenario.enums.ScenarioStatus;

/**
 * 시나리오 조회/생성/수정/삭제를 담당하는 서비스 인터페이스
 */
public interface ScenarioService {
    UserScenarioResponse getScenarioForUser(Long battleId, Long userId);
    AdminScenarioDetailResponse getScenarioForAdmin(Long battleId);
    Long createScenario(ScenarioCreateRequest request);
    void updateScenarioContent(Long scenarioId, ScenarioCreateRequest request);
    AdminScenarioResponse updateScenarioStatus(Long scenarioId, ScenarioStatus status);
    AdminDeleteResponse deleteScenario(Long scenarioId);
}
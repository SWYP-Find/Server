package com.swyp.picke.domain.battle.dto.parse;

import com.swyp.picke.domain.admin.dto.battle.request.AdminBattleCreateRequest;
import com.swyp.picke.domain.admin.dto.scenario.request.AdminScenarioCreateRequest;
import java.util.List;
import java.util.Map;

/**
 * 붙여넣기 파싱 결과. DB 에 저장하지 않고 미리보기로만 돌려준다.
 *
 * <p>{@code scenarioPayload.battleId} 는 null 이다 — 배틀이 아직 없어서다.
 * 어드민이 {@code battlePayload} 로 배틀을 먼저 만든 뒤, 받은 id 를 채워 시나리오를 등록한다.
 *
 * <p>{@code speakerNames} 는 화자(A/B)별 발화자 철학자 이름. voiceSettings 와 함께 미리보기에 표시한다.
 */
public record AdminBattleParseResponse(
        AdminBattleCreateRequest battlePayload,
        AdminScenarioCreateRequest scenarioPayload,
        Map<String, String> speakerNames,
        List<ParseWarning> warnings
) {}

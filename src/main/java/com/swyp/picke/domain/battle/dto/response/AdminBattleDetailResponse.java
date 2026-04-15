package com.swyp.picke.domain.battle.dto.response;

import com.swyp.picke.domain.battle.enums.BattleCreatorType;
import com.swyp.picke.domain.battle.enums.BattleStatus;
import com.swyp.picke.domain.battle.enums.BattleType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 관리자 - 배틀 상세 상세 조회 응답
 * 역할: 관리자가 배틀의 모든 설정 값(상태, 생성자 타입, 수정일 등)을 확인하고 수정할 때 사용합니다.
 */

public record AdminBattleDetailResponse(
        Long battleId,
        String title,
        String titlePrefix,
        String titleSuffix,
        String summary,
        String description,
        String thumbnailUrl,
        BattleType type,
        String itemA,
        String itemADesc,
        String itemB,
        String itemBDesc,
        LocalDate targetDate,
        BattleStatus status,
        BattleCreatorType creatorType,
        List<BattleTagResponse> tags,
        List<BattleOptionResponse> options,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
package com.swyp.picke.domain.admin.dto.battle.response;

import com.swyp.picke.domain.battle.dto.response.BattleOptionResponse;
import com.swyp.picke.domain.battle.dto.response.BattleTagResponse;
import com.swyp.picke.domain.battle.enums.BattleCreatorType;
import com.swyp.picke.domain.battle.enums.BattleStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 관리자 배틀 상세 조회 응답
 */
public record AdminBattleDetailResponse(
        Long battleId,
        String title,
        String summary,
        String description,
        String thumbnailUrl,
        Integer audioDuration,
        LocalDate targetDate,
        BattleStatus status,
        BattleCreatorType creatorType,
        List<BattleTagResponse> tags,
        List<BattleOptionResponse> options,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}

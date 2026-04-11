package com.swyp.picke.domain.admin.dto.battle.response;

import java.time.LocalDateTime;

/**
 * 관리자 배틀 삭제 응답
 */
public record AdminBattleDeleteResponse(
        Boolean success,
        LocalDateTime deletedAt
) {}
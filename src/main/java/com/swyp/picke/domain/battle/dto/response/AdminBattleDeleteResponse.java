package com.swyp.picke.domain.battle.dto.response;

import java.time.LocalDateTime;

/**
 * 관리자 - 배틀 삭제 응답
 * 역할: 배틀이 성공적으로 소프트 딜리트 되었는지 확인하고 삭제 시점을 반환합니다.
 */

public record AdminBattleDeleteResponse(
        Boolean success, // 삭제 성공 여부
        LocalDateTime deletedAt // 삭제 처리된 일시 (Soft Delete)
) {}
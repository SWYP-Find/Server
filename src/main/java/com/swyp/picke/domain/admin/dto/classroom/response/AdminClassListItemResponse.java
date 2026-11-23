package com.swyp.picke.domain.admin.dto.classroom.response;

import com.swyp.picke.domain.classroom.enums.ClassBattleType;
import java.time.LocalDateTime;

public record AdminClassListItemResponse(
        Long classId,
        String name,
        String ownerNickname,
        Long battleId,
        String selectedContent,
        String thumbnailUrl,
        ClassBattleType battleType,
        long participantCount,
        int capacity,
        LocalDateTime closeAt,
        AdminClassState state,
        LocalDateTime createdAt
) {
}

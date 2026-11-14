package com.swyp.picke.domain.admin.dto.classroom.response;

import com.swyp.picke.domain.admin.dto.classroom.AdminClassState;
import com.swyp.picke.domain.classroom.enums.ClassBattleType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/** 클래스 관리 목록 행. */
public record AdminClassListItemResponse(
        Long classId,
        String name,
        String ownerNickname,
        Long battleId,
        @Schema(description = "화면의 선택 콘텐츠 이름") String selectedContent,
        String thumbnailUrl,
        ClassBattleType battleType,
        long participantCount,
        int capacity,
        LocalDateTime closeAt,
        AdminClassState state,
        LocalDateTime createdAt
) {
}

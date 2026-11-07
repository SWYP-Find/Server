package com.swyp.picke.domain.classroom.dto.response;

import com.swyp.picke.domain.classroom.enums.ClassBattleType;
import com.swyp.picke.domain.classroom.enums.ClassMemberRole;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "내 클래스 목록 항목")
public record ClassSummaryResponse(
        @Schema(description = "클래스 ID") Long classId,
        @Schema(description = "클래스 이름") String name,
        @Schema(description = "내 역할 (OWNER / MEMBER)") ClassMemberRole myRole,
        @Schema(description = "배틀 ID") Long battleId,
        @Schema(description = "배틀 제목") String battleTitle,
        @Schema(description = "배틀 썸네일 URL") String battleThumbnailUrl,
        @Schema(description = "배틀 종류 (TTS / CUSTOM)") ClassBattleType battleType,
        @Schema(description = "좌석을 차지한 인원 (운영자 참여 시 포함)") long memberCount,
        @Schema(description = "정원") int capacity,
        @Schema(description = "실제 마감 시각 = min(마감일, 이용권 종료일)") LocalDateTime closeAt,
        @Schema(description = "마감 여부") boolean closed,
        @Schema(description = "생성 시각") LocalDateTime createdAt
) {
}

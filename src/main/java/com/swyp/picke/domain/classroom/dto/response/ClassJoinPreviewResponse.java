package com.swyp.picke.domain.classroom.dto.response;

import com.swyp.picke.domain.classroom.enums.ClassBattleType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "참여 코드 확인 결과. 참여할 수 있는 코드일 때만 내려간다")
public record ClassJoinPreviewResponse(
        @Schema(description = "정리된 초대코드 6자 (참여 API 에 그대로 보낸다)") String inviteCode,
        @Schema(description = "클래스 이름") String name,
        @Schema(description = "배틀 제목") String battleTitle,
        @Schema(description = "배틀 썸네일") String battleThumbnailUrl,
        @Schema(description = "배틀 종류 (TTS / CUSTOM)") ClassBattleType battleType,
        @Schema(description = "좌석을 차지한 인원") long memberCount,
        @Schema(description = "정원") int capacity,
        @Schema(description = "실제 마감 시각 = min(마감일, 이용권 종료일)") LocalDateTime closeAt
) {
}

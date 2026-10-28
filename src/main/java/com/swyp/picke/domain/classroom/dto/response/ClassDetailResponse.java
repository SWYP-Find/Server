package com.swyp.picke.domain.classroom.dto.response;

import com.swyp.picke.domain.classroom.enums.ClassBattleType;
import com.swyp.picke.domain.classroom.enums.ClassMemberRole;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "클래스 상세")
public record ClassDetailResponse(
        @Schema(description = "클래스 ID") Long classId,
        @Schema(description = "클래스 이름") String name,
        @Schema(description = "초대코드 6자. 공유하기 버튼용으로 운영자·멤버 모두에게 내려간다") String inviteCode,
        @Schema(description = "내 역할 (OWNER / MEMBER)") ClassMemberRole myRole,
        @Schema(description = "운영자 유저 ID") Long ownerId,
        @Schema(description = "배틀 정보") BattleInfo battle,
        @Schema(description = "배틀 종류 (TTS / CUSTOM)") ClassBattleType battleType,
        @Schema(description = "커스텀 배틀의 주제 버전 ID. TTS 클래스는 null", nullable = true) Long topicVersionId,
        @Schema(description = "좌석을 차지한 인원 (운영자 참여 시 포함)") long memberCount,
        @Schema(description = "정원") int capacity,
        @Schema(description = "운영자가 설정한 마감일. null 이면 마감 없음") LocalDateTime deadline,
        @Schema(description = "이용권 종료일") LocalDateTime ticketExpiresAt,
        @Schema(description = "실제 마감 시각 = min(마감일, 이용권 종료일)") LocalDateTime closeAt,
        @Schema(description = "마감 여부") boolean closed,
        @Schema(description = "댓글 작성 필수 여부") boolean commentRequired,
        @Schema(description = "운영자 참여 여부") boolean ownerParticipates,
        @Schema(description = "생성 시각") LocalDateTime createdAt
) {
    @Schema(description = "클래스에 붙은 배틀 요약")
    public record BattleInfo(
            Long battleId,
            String title,
            String summary,
            String thumbnailUrl
    ) {
    }
}

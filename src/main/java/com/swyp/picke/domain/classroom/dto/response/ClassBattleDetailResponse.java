package com.swyp.picke.domain.classroom.dto.response;

import com.swyp.picke.domain.battle.dto.response.BattleUserDetailResponse;
import com.swyp.picke.domain.classroom.enums.ClassMemberRole;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "클래스 안에서 보는 배틀 상세. 투표 상태·단계·참여자 수는 이 클래스 안의 것만 반영한다")
public record ClassBattleDetailResponse(
        @Schema(description = "클래스 ID") Long classId,
        @Schema(description = "내 역할 (OWNER / MEMBER)") ClassMemberRole myRole,
        @Schema(description = "실제 마감 시각 = min(마감일, 이용권 종료일)") LocalDateTime closeAt,
        @Schema(description = "마감 여부. true 면 투표·의견·댓글 쓰기가 막힌다") boolean closed,
        @Schema(description = "의견 작성 필수 여부") boolean commentRequired,
        @Schema(description = "지금 투표·의견을 쓸 수 있는지 (참여 중인 멤버이고 마감 전). 참여하지 않는 운영자는 false") boolean canParticipate,
        @Schema(description = "참여 완료 여부. 사후투표 완료 + (의견 필수면) 내 의견 1건 이상") boolean participationCompleted,
        @Schema(description = "중간투표가 있는 기존 콘텐츠인지. true 면 콘텐츠의 선택 화면에서 고른 선택지를 POST /votes/mid 로 보낸다") boolean midVoteEnabled,
        @Schema(description = "배틀 상세. userVoteStatus / currentStep / participantsCount 는 클래스 기준") BattleUserDetailResponse battle
) {
}

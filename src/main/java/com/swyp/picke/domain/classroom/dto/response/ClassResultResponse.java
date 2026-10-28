package com.swyp.picke.domain.classroom.dto.response;

import com.swyp.picke.domain.classroom.enums.ClassMemberRole;
import com.swyp.picke.domain.classroom.enums.ClassReportViewState;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "클래스 결과 화면. 정량 통계는 마감 전이면 현재 데이터(LIVE), 마감 후면 마감 시점 스냅샷(SNAPSHOT)")
public record ClassResultResponse(
        @Schema(description = "클래스 ID") Long classId,
        @Schema(description = "내 역할 (OWNER / MEMBER)") ClassMemberRole myRole,
        @Schema(description = "실제 마감 시각 = min(마감일, 이용권 종료일)") LocalDateTime closeAt,
        @Schema(description = "마감 여부") boolean closed,
        @Schema(description = "결과 열람 가능 기한 (이용권 종료 + 30일). 지나면 409 CLASS_409_RESULT_EXPIRED") LocalDateTime viewableUntil,
        @Schema(description = "통계 기준. LIVE = 현재 데이터, SNAPSHOT = 마감 스냅샷") String statsBasis,
        @Schema(description = "스냅샷 기준 시각. LIVE 면 null") LocalDateTime cutoffAt,
        @Schema(description = "정량 통계") Stats stats,
        @Schema(description = "내 AI 피드백 상태. 참여 멤버가 아니면 null (참여하지 않는 운영자)") ClassReportViewState myReportState,
        @Schema(description = "클래스 인사이트 상태. 운영자만, 아니면 null") ClassReportViewState insightState
) {

    @Schema(description = "정량 통계. 비율은 소수 첫째 자리 %, 분모 0 이면 0")
    public record Stats(
            @Schema(description = "활성 참여자 수") int activeMemberCount,
            @Schema(description = "참여 완료 인원") int completedCount,
            @Schema(description = "참여율 % = 완료 / 활성") double participationRate,
            @Schema(description = "의견 작성자 수") int commentWriterCount,
            @Schema(description = "댓글 작성률 % = 작성자 / 활성") double commentRate,
            @Schema(description = "사후투표 인원") int postVoteCount,
            @Schema(description = "중간투표가 있는 기존 콘텐츠인지. false 면 중간투표 값은 모두 0 이고 화면에서 숨긴다") boolean midVoteEnabled,
            @Schema(description = "중간투표 인원") int midVoteCount,
            @Schema(description = "선택지별 사전/중간/사후 분포") List<OptionStat> options
    ) {
    }

    @Schema(description = "선택지별 분포. postVoteRatio 는 사후투표자 중 비율")
    public record OptionStat(
            Long optionId,
            String title,
            String stance,
            int preVoteCount,
            @Schema(description = "중간투표 수. midVoteEnabled 가 false 면 0") int midVoteCount,
            int postVoteCount,
            double postVoteRatio
    ) {
    }
}

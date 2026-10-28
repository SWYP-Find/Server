package com.swyp.picke.domain.classroom.dto.response;

import com.swyp.picke.domain.classroom.enums.ClassReportViewState;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "내 AI 피드백. state 가 SUCCEEDED / LIMITED 일 때만 feedback 이 있다")
public record ClassMyReportResponse(
        @Schema(description = "클래스 ID") Long classId,
        @Schema(description = "상태. BEFORE_CLOSE = 마감 전(\"{closeAt} 이후 AI 피드백이 제공돼요\"), NOT_ELIGIBLE = 참여 미완료, "
                + "PENDING/GENERATING = 생성 중, SUCCEEDED = AI 피드백, LIMITED = 글이 없어 템플릿, FAILED = 생성 실패") ClassReportViewState state,
        @Schema(description = "실제 마감 시각") LocalDateTime closeAt,
        @Schema(description = "사전투표 선택지") Option preOption,
        @Schema(description = "선택 지점별 중간투표. 중간투표가 없는 배틀이거나 고르지 않았으면 빈 배열") List<ClassMidVoteInfo> midVotes,
        @Schema(description = "사후투표 선택지") Option postOption,
        @Schema(description = "이 클래스에서 내가 쓴 의견 수") int perspectiveCount,
        @Schema(description = "이 클래스에서 내가 쓴 댓글 수") int commentCount,
        @Schema(description = "피드백 본문. 상태가 SUCCEEDED / LIMITED 가 아니면 null") Feedback feedback,
        @Schema(description = "생성 시각") LocalDateTime generatedAt
) {

    public record Option(Long optionId, String title, String stance) {
    }

    @Schema(description = "PRD §13 출력 구조. strength / nextFocus 는 기록으로 확인되지 않으면 null")
    public record Feedback(
            @Schema(description = "내 판단의 기준 한 문장") String headline,
            @Schema(description = "판단 기준과 변화 (150자 이하)") String judgmentBasis,
            @Schema(description = "잘한 점 1개 또는 null") Section strength,
            @Schema(description = "다음에 해볼 것 1개 또는 null") Section nextFocus,
            @Schema(description = "다음 질문") String nextQuestion,
            @Schema(description = "근거가 된 내 발언. 원문은 현재 상태로 다시 조회한다") List<Evidence> evidence,
            @Schema(description = "SUFFICIENT / LIMITED") String analysisStatus,
            @Schema(description = "full / partial (분량 제한으로 일부 글 제외)") String coverage,
            @Schema(description = "분량 제한으로 제외된 글 수") int excludedCount
    ) {
    }

    public record Section(String title, String body) {
    }

    @Schema(description = "근거 발언. field = judgment_basis / strength / next_focus")
    public record Evidence(
            String field,
            @Schema(description = "PERSPECTIVE / COMMENT") String sourceType,
            Long sourceId,
            @Schema(description = "피드백이 인용한 구절") String quote,
            @Schema(description = "발언 원문 (현재 기준). 삭제·숨김이면 목록에서 빠진다") String content,
            LocalDateTime createdAt
    ) {
    }
}

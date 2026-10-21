package com.swyp.picke.domain.classroom.dto.response;

import com.swyp.picke.domain.classroom.enums.ClassDebateFocus;
import com.swyp.picke.domain.classroom.enums.ClassDebateSpeaker;
import com.swyp.picke.domain.classroom.enums.ClassDebateStatus;
import com.swyp.picke.domain.user.enums.PhilosopherType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "AI 철학자 토론 세션. 재접속하면 저장된 대화를 그대로 돌려준다")
public record ClassDebateResponse(
        @Schema(description = "세션 ID") Long sessionId,
        @Schema(description = "GENERATING = AI 주장 생성 중(\"생각을 정리하고 있어요\"), WAITING_USER = 반박 대기, "
                + "FAILED = AI 주장 생성 실패(재시도 API), COMPLETED = 3라운드 종료(대화 다시보기 또는 사후투표)") ClassDebateStatus status,
        @Schema(description = "현재 라운드 (1~3). 생성 중이거나 반박을 기다리는 라운드") int round,
        @Schema(description = "지금 반박을 보낼 수 있는지 (WAITING_USER 이고 마감 전)") boolean canReply,
        @Schema(description = "지금 재시도할 수 있는지 (FAILED 또는 오래 멈춘 생성)") boolean canRetry,
        @Schema(description = "AI 가 응용하는 철학자") Philosopher philosopher,
        @Schema(description = "참가자의 사전투표 선택지") Side participantOption,
        @Schema(description = "AI 가 대변하는 반대 선택지") Side opponentOption,
        @Schema(description = "라운드순 대화. 라운드마다 AI 주장 → 참가자 반박") List<Message> messages,
        @Schema(description = "AI 주장에 함께 표시할 안내 문구") String aiNotice,
        @Schema(description = "반박 최소 글자 수") int replyMinLength,
        @Schema(description = "반박 최대 글자 수") int replyMaxLength
) {

    public record Philosopher(PhilosopherType code, String name, String typeName, String imageKey) {
    }

    public record Side(Long optionId, @Schema(description = "A / B") String position, String title, String stance) {
    }

    public record Message(
            Long messageId,
            int round,
            ClassDebateSpeaker speaker,
            String content,
            @Schema(description = "AI 주장의 초점. 참가자 반박은 null") ClassDebateFocus focus,
            @Schema(description = "초점 한글 이름 (근거·가정·반례·조건·가치충돌)") String focusLabel,
            @Schema(description = "AI 주장이 응답한 참가자 반박 ID") Long replyToMessageId,
            LocalDateTime createdAt
    ) {
    }
}

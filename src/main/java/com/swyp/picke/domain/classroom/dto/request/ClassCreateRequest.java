package com.swyp.picke.domain.classroom.dto.request;

import com.swyp.picke.domain.classroom.entity.ClassMember;
import com.swyp.picke.domain.classroom.entity.Classroom;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

@Schema(description = "클래스 생성 요청")
public record ClassCreateRequest(

        @Schema(description = "클래스 이름 (1~20자)", example = "3학년 2반 철학 토론")
        @NotBlank
        @Size(max = Classroom.NAME_MAX_LENGTH)
        String name,

        @Schema(description = "클래스에서 진행할 배틀 ID (기존 TTS 배틀)", example = "12")
        @NotNull
        Long battleId,

        @Schema(description = "참여 마감일. null 이면 마감 없음(이용권 종료일까지). 현재 이후 ~ 이용권 종료일 범위만 허용",
                example = "2026-12-31T23:59:59", nullable = true)
        LocalDateTime deadline,

        @Schema(description = "댓글 작성 필수 여부", example = "true")
        boolean commentRequired,

        @Schema(description = "운영자 본인도 참여자로 들어갈지 여부. true 면 운영자가 1석을 차지한다", example = "true")
        boolean ownerParticipates,

        @Schema(description = "운영자가 참여할 때 클래스 안에서 쓸 이름 (1~20자). 비우면 닉네임 사용",
                example = "김선생", nullable = true)
        @Size(max = ClassMember.DISPLAY_NAME_MAX_LENGTH)
        String ownerDisplayName
) {
}

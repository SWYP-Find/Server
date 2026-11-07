package com.swyp.picke.domain.classroom.dto.request;

import com.swyp.picke.domain.classroom.entity.ClassMember;
import com.swyp.picke.domain.classroom.entity.Classroom;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

@Schema(description = "클래스 생성 요청")
public record ClassCreateRequest(

        @Schema(description = "클래스 이름 (1~20자)", example = "3학년 2반 철학 토론")
        @NotBlank
        @Size(max = Classroom.NAME_MAX_LENGTH)
        String name,

        @Schema(description = "기존 콘텐츠(TTS) 배틀 ID. topicVersionId 와 둘 중 하나만 보낸다", example = "12", nullable = true)
        Long battleId,

        @Schema(description = "커스텀 배틀로 만들 때 검증된 주제 버전 ID (주제 초안 API 의 topicVersionId). battleId 와 둘 중 하나만 보낸다",
                example = "3", nullable = true)
        Long topicVersionId,

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
        String ownerDisplayName,

        @Schema(description = "중복 생성 방지용 요청 키 (1~64자, 영문·숫자·-·_). 생성 화면을 열 때 UUID 하나를 만들어 재전송에도 같은 값을 보낸다. "
                + "같은 키로 같은 내용을 다시 보내면 기존 클래스를 돌려주고, 다른 내용이면 409",
                example = "3f2b8c1e-7d4a-4f6b-9a0e-2c5d8e1f4a7b", nullable = true)
        @Size(max = Classroom.CREATE_REQUEST_KEY_MAX_LENGTH)
        @Pattern(regexp = "^[A-Za-z0-9_-]+$")
        String requestKey
) {
}

package com.swyp.picke.domain.classroom.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "내 표시 이름 변경 요청")
public record ClassMemberNameUpdateRequest(

        @Schema(description = "새 표시 이름 (앞뒤 공백 제거 후 1~20자, 같은 클래스 안에서 중복 불가)", example = "김학생2")
        @NotBlank
        String displayName
) {
}

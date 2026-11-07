package com.swyp.picke.domain.classroom.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/** 초대코드는 공백 제거·대문자 변환 후 6자, 표시 이름은 앞뒤 공백 제거 후 1~20자 (서비스에서 검사). */
@Schema(description = "클래스 참여 요청")
public record ClassJoinRequest(

        @Schema(description = "초대코드 6자. 공백·대소문자는 서버가 정리한다", example = "A7K2MP")
        @NotBlank
        String inviteCode,

        @Schema(description = "클래스 안에서 쓸 이름 (앞뒤 공백 제거 후 1~20자, 같은 클래스 안에서 중복 불가)", example = "김학생")
        @NotBlank
        String displayName
) {
}

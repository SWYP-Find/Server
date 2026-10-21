package com.swyp.picke.domain.classroom.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "AI 후보 선택 요청. 화면에 보인 배치 번호를 함께 보내 이미 바뀐 후보를 고르지 않게 한다")
public record ClassTopicSelectRequest(

        @Schema(description = "후보가 속한 배치 번호 (초안 응답의 batchNo)", example = "1")
        @NotNull
        Integer batchNo,

        @Schema(description = "후보 ID (1~3)", example = "3")
        @NotNull
        Integer candidateId
) {
}

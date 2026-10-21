package com.swyp.picke.domain.classroom.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "주제 직접 수정 검증 결과 (부록 A). valid=false 면 새 버전을 만들지 않고 마지막 검증된 버전이 유지된다")
public record ClassTopicValidationResponse(
        boolean valid,
        @Schema(description = "필드별 문제. field = question / description / options[i].label / options[i].rationale / options[i].philosopher / topic")
        List<Issue> issues,
        @Schema(description = "검증을 통과해 저장된 주제 버전 ID. valid=false 면 null", nullable = true) Long validatedTopicVersionId,
        @Schema(description = "남은 수정 검증 횟수") int remainingValidations,
        @Schema(description = "저장된 주제 버전", nullable = true) ClassTopicVersionResponse version
) {

    public record Issue(String field, String code, String message) {
    }
}

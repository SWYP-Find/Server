package com.swyp.picke.domain.classroom.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "클래스 중간투표 요청")
public record ClassMidVoteRequest(

        @Schema(description = "선택 지점 ID. 시나리오 응답에서 선택지(interactiveOptions)가 있는 노드의 nodeId", example = "15")
        Long checkpointId,

        @Schema(description = "고른 선택지 ID. 시나리오 응답 interactiveOptions[].optionId", example = "3")
        Long optionId
) {
}

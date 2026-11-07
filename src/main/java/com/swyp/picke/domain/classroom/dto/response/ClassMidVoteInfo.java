package com.swyp.picke.domain.classroom.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "선택 지점별 내 중간투표")
public record ClassMidVoteInfo(
        @Schema(description = "선택 지점 ID (시나리오 nodeId)") Long checkpointId,
        @Schema(description = "고른 선택지 ID (시나리오 optionId)") Long optionId,
        @Schema(description = "고른 선택지 문구") String label
) {
}

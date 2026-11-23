package com.swyp.picke.domain.admin.dto.battle.response;

import io.swagger.v3.oas.annotations.media.Schema;

/** 콘텐츠 관리 화면 상단 통계. */
public record AdminBattleCatalogSummaryResponse(
        @Schema(description = "삭제되지 않은 관리자·AI 콘텐츠 수") long totalContents,
        @Schema(description = "난이도가 지정되지 않은 콘텐츠 수") long difficultyMissing,
        @Schema(description = "카테고리가 지정되지 않은 콘텐츠 수") long categoryMissing,
        @Schema(description = "클래스 추천이 활성화된 콘텐츠 수") long classRecommended
) {
}

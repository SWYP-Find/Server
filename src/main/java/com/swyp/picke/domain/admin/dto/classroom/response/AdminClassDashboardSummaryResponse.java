package com.swyp.picke.domain.admin.dto.classroom.response;

import io.swagger.v3.oas.annotations.media.Schema;

/** 클래스 관리 목록 상단 요약 카드. */
public record AdminClassDashboardSummaryResponse(
        @Schema(description = "삭제되지 않은 클래스 수") long totalClasses,
        @Schema(description = "현재 참여 가능한 클래스 수") long ongoingClasses,
        @Schema(description = "삭제되지 않은 클래스에서 좌석을 점유 중인 전체 인원") long participants,
        @Schema(description = "마감까지 30일 이내인 진행 중 클래스 수") long closingWithin30Days
) {
}

package com.swyp.picke.domain.admin.dto.classroom.response;

public record AdminClassSummaryResponse(
        long totalClasses,
        long ongoingClasses,
        long participants,
        long closingWithin30Days
) {
}

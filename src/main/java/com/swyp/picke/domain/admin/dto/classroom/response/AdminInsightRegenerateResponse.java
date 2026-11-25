package com.swyp.picke.domain.admin.dto.classroom.response;

import com.swyp.picke.domain.classroom.enums.ClassReportStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "클래스 인사이트 재생성 요청 결과 (#705)")
public record AdminInsightRegenerateResponse(
        Long classId,
        Long reportId,
        @Schema(description = "PENDING 이면 1분 안에 스케줄러가 다시 만든다") ClassReportStatus status
) {
}

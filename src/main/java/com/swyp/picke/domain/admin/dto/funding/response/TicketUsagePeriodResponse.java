package com.swyp.picke.domain.admin.dto.funding.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "사용 기간 설정 결과")
public record TicketUsagePeriodResponse(
        @Schema(description = "바뀐 이용권 ID") List<Long> ticketIds,
        @Schema(description = "설정한 사용 기간(일). null 이면 기본 6개월", nullable = true) Integer usageDays
) {
}

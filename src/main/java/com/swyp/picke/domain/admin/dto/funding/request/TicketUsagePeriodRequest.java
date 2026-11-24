package com.swyp.picke.domain.admin.dto.funding.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

@Schema(description = "등록 전 이용권의 사용 기간(일) 일괄 설정 (#685)")
public record TicketUsagePeriodRequest(
        @Schema(description = "대상 이용권 ID. 모두 등록 전(ISSUED/DELIVERED)이어야 한다", example = "[12, 13]")
        @NotEmpty @Size(max = 200) List<Long> ticketIds,

        @Schema(description = "사용 기간(일). 등록 시 만료일 = 등록 시각 + 이 일수. null 이면 기본 6개월로 되돌린다",
                example = "14", nullable = true)
        @Min(1) @Max(183) Integer usageDays
) {
}

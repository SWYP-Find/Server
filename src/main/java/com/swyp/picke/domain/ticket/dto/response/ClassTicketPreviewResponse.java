package com.swyp.picke.domain.ticket.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/** 코드 확인 화면(P01)용. 코드를 소비하지 않고 정원·등록 기한만 보여준다. */
public record ClassTicketPreviewResponse(
        @Schema(description = "클래스 정원(좌석 수)") int capacity,
        @Schema(description = "이 코드를 등록할 수 있는 마지막 시각 (결제 확정일 + 1년)") LocalDateTime registerBy,
        @Schema(description = "등록 시점부터의 이용 기간(개월). 일 단위 이용권은 올림한 개월 수(14일 → 1). 정확한 표시는 usageLabel") int usageMonths,
        @Schema(description = "일 단위 사용 기간 (#691). 기본 6개월 이용권이면 null", nullable = true) Integer usageDays,
        @Schema(description = "화면 표시용 사용 기간 (예: 6개월, 14일)", example = "6개월") String usageLabel,
        @Schema(description = "내가 이미 등록한 코드면 true. 이 경우 등록 대신 내 이용권 조회로 보내면 된다") boolean registeredByMe
) {}

package com.swyp.picke.domain.admin.dto.funding.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/** CSV 업로드 결과. 행 단위 오류는 errors 로 모아 돌려주고, 나머지 행은 정상 반영된다. */
public record FundingImportResponse(
        @Schema(description = "업로드한 파일의 SHA-256 (import_file_hash 에 기록)") String fileHash,
        @Schema(description = "빈 줄을 제외한 데이터 행 수") int totalRows,
        @Schema(description = "새로 추가된 주문 수") int created,
        @Schema(description = "값이 바뀌어 갱신된 주문 수") int updated,
        @Schema(description = "값 변화가 없던 주문 수") int unchanged,
        @Schema(description = "반영하지 못한 행 수 (= errors.size())") int errorCount,
        @Schema(description = "업로드 후 기준, 지금 발급 가능한 주문 수 (결제 확정·수량 1·미발급)") long issuableCount,
        @Schema(description = "보류 주문 수 (결제 확정이지만 수량이 1이 아니거나 결제일 없음, 결제 대기·예약)") long heldCount,
        List<RowError> errors
) {
    public record RowError(
            @Schema(description = "CSV 행 번호 (헤더 = 1)") int line,
            @Schema(nullable = true) String orderId,
            String reason
    ) {}
}

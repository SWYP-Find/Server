package com.swyp.picke.domain.funding.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "자동 열 판별을 적용한 텀블벅 명단 미리보기. 미리보기에서는 저장·코드 발급을 하지 않습니다.")
public record TumblbugImportPreviewResponse(
        String sheetName,
        int headerRowNumber,
        List<String> detectedColumns,
        int totalRows,
        int importableRows,
        int codeEligibleRows,
        int duplicateRows,
        int invalidRows,
        List<TumblbugImportRowResponse> rows
) {
}

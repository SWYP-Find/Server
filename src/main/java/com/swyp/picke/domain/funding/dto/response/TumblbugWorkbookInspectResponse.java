package com.swyp.picke.domain.funding.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Map;

@Schema(description = "텀블벅 XLSX 시트별 자동 헤더 판별 결과와 샘플")
public record TumblbugWorkbookInspectResponse(List<SheetSample> sheets) {
    public record SheetSample(
            String sheetName,
            int lastRowNumber,
            Integer headerRowNumber,
            Map<String, String> detectedColumns,
            List<String> missingColumns,
            List<List<String>> sampleRows
    ) { }
}

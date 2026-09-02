package com.swyp.picke.domain.ad.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

@Schema(description = "광고 노출 집계 요청")
public record AdImpressionRequest(

        @Schema(description = "실제로 화면에 노출된 소재 코드 목록", example = "[\"a1b2c3d4\"]")
        @NotEmpty(message = "노출된 소재 코드는 최소 1개 이상이어야 합니다.")
        List<String> codes
) {
}

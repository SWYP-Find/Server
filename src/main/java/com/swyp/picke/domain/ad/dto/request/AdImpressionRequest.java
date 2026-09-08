package com.swyp.picke.domain.ad.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * 노출 집계는 익명 공개 API다. 아무나 반복 호출해 노출수를 부풀릴 수 있다는 것을 감수하는 대신,
 * 한 번에 밀어 넣을 수 있는 양은 막아 둔다. 한 화면에 그려지는 소재 수가 이보다 많을 일은 없다.
 */
@Schema(description = "광고 노출 집계 요청")
public record AdImpressionRequest(

        @Schema(description = "실제로 화면에 노출된 소재 코드 목록", example = "[\"a1b2c3d4\"]")
        @NotEmpty(message = "노출된 소재 코드는 최소 1개 이상이어야 합니다.")
        @Size(max = MAX_CODES, message = "한 번에 집계할 수 있는 소재는 최대 20개입니다.")
        List<String> codes
) {

    private static final int MAX_CODES = 20;
}

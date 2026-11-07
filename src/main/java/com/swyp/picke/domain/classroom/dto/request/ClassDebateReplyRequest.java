package com.swyp.picke.domain.classroom.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** 반박 길이(앞뒤 공백 제거 후 50~500자)는 서비스에서 검사한다. */
@Schema(description = "토론 반박 제출 요청")
public record ClassDebateReplyRequest(

        @Schema(description = "반박할 라운드 (1~3). 화면에 보인 현재 라운드를 보낸다", example = "1")
        @NotNull
        Integer roundIndex,

        @Schema(description = "반박 내용 (앞뒤 공백 제거 후 50자 이상 500자 이하). 저장 후 수정할 수 없다",
                example = "속으로 슬퍼해도 밖으로 표현하지 않을 수 있어요. 표현 방식은 사람마다 달라서 겉모습만으로 감정을 판단하면 안 된다고 생각해요.")
        String content,

        @Schema(description = "중복 전송 방지 키. 같은 반박을 다시 보낼 때는 같은 값을 쓴다 (UUID 권장)", example = "4f0b5c3e-6a8d-4f6b-9a51-2f3d1c7e9b10")
        @NotBlank
        @Size(max = 64)
        String requestKey
) {
}

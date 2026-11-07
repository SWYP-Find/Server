package com.swyp.picke.domain.classroom.dto.request;

import com.swyp.picke.domain.battle.enums.BattleCategory;
import com.swyp.picke.domain.battle.enums.BattleDifficulty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/** 키워드는 앞뒤 공백 제거 후 2~200자, 카테고리는 중복 제거 후 1~3개 (서비스에서 검사). */
@Schema(description = "커스텀 배틀 주제 초안 생성 요청")
public record ClassTopicDraftCreateRequest(

        @Schema(description = "다루고 싶은 주제·관심사 (2~200자). 작품을 다루면 필요한 맥락도 함께 적는다",
                example = "카뮈 『이방인』의 뫼르소처럼 어머니 장례식에서 울지 않은 사람을 비난할 수 있을까")
        @NotBlank
        String topicSeed,

        @Schema(description = "대상 수준", example = "INTERMEDIATE")
        @NotNull
        BattleDifficulty difficulty,

        @Schema(description = "카테고리 1~3개 (철학·사회·문학·과학·예술·역사)", example = "[\"철학\", \"문학\"]")
        @NotEmpty
        List<BattleCategory> categories
) {
}

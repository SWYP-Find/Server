package com.swyp.picke.domain.classroom.dto.request;

import com.swyp.picke.domain.battle.enums.BattleCategory;
import com.swyp.picke.domain.battle.enums.BattleDifficulty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/** 재추천 시 조건을 바꿀 때만 채운다. null 필드는 초안의 기존 조건을 쓴다. */
@Schema(description = "AI 주제 제안(재추천) 요청. 본문을 생략하면 초안의 조건 그대로 제안한다")
public record ClassTopicSuggestRequest(

        @Schema(description = "바꿀 주제·관심사 (2~200자)", nullable = true)
        String topicSeed,

        @Schema(description = "바꿀 대상 수준", nullable = true)
        BattleDifficulty difficulty,

        @Schema(description = "바꿀 카테고리 1~3개", nullable = true)
        List<BattleCategory> categories
) {
}

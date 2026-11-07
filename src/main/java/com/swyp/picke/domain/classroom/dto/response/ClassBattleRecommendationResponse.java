package com.swyp.picke.domain.classroom.dto.response;

import com.swyp.picke.domain.battle.enums.BattleCategory;
import com.swyp.picke.domain.battle.enums.BattleDifficulty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "클래스 Picke 배틀 추천 결과. 최대 3개이고 조건에 맞는 배틀이 적으면 적은 대로 내려간다")
public record ClassBattleRecommendationResponse(

        @Schema(description = "추천 배틀 (0~3개)") List<Item> items
) {

    @Schema(description = "추천 배틀 카드")
    public record Item(
            Long battleId,
            String title,
            String summary,
            String thumbnailUrl,
            @Schema(description = "카테고리 (철학·사회·문학·과학·예술·역사)") BattleCategory category,
            @Schema(description = "대상 수준") BattleDifficulty difficulty,
            @Schema(description = "소요 시간(초)") Integer audioDuration,
            @Schema(description = "선택지 2개 (A, B 순서). 미리듣기 음성은 GET /api/v1/battles/{battleId}/scenario 로 받는다") List<Option> options
    ) {
    }

    public record Option(
            Long optionId,
            @Schema(description = "A / B") String position,
            String title,
            String stance,
            @Schema(description = "대표 철학자") String representative,
            String imageUrl
    ) {
    }
}

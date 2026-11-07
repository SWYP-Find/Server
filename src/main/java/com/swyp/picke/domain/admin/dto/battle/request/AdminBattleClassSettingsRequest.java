package com.swyp.picke.domain.admin.dto.battle.request;

import com.swyp.picke.domain.battle.enums.BattleCategory;
import com.swyp.picke.domain.battle.enums.BattleDifficulty;
import io.swagger.v3.oas.annotations.media.Schema;

/** null 인 필드는 변경하지 않는다. 클래스 추천을 켜려면 수준·카테고리가 모두 정해져 있어야 한다. */
@Schema(description = "배틀 클래스 설정 수정 요청 (null 필드는 변경 없음)")
public record AdminBattleClassSettingsRequest(

        @Schema(description = "대상 수준", example = "BEGINNER", nullable = true)
        BattleDifficulty difficulty,

        @Schema(description = "카테고리 (철학·사회·문학·과학·예술·역사)", example = "철학", nullable = true)
        BattleCategory category,

        @Schema(description = "클래스 추천 배틀로 노출할지", example = "true", nullable = true)
        Boolean classRecommendable,

        @Schema(description = "중간투표 콘텐츠 여부", example = "false", nullable = true)
        Boolean midVoteEnabled
) {
}

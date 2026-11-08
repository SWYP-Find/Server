package com.swyp.picke.domain.admin.dto.battle.request;

import com.swyp.picke.domain.battle.enums.BattleStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/** 공개(PUBLISHED) ↔ 보관(ARCHIVED) 전환만 받는다. 임시저장 배틀의 첫 발행은 배틀 수정(만들기)으로 한다 (#622) */
@Schema(description = "배틀 공개·보관 상태 변경 요청")
public record AdminBattleStatusRequest(

        @Schema(description = "바꿀 상태. PUBLISHED(공개) 또는 ARCHIVED(보관)", example = "ARCHIVED")
        @NotNull
        BattleStatus status
) {
}

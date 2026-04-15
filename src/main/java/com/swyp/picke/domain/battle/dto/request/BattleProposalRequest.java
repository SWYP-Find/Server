package com.swyp.picke.domain.battle.dto.request;

import com.swyp.picke.domain.battle.enums.BattleCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class BattleProposalRequest {

    @NotNull(message = "카테고리를 선택해주세요")
    private BattleCategory category;

    @NotBlank(message = "주제를 입력해주세요")
    @Size(max = 100, message = "주제는 100자 이내로 입력해주세요")
    private String topic;

    @NotBlank(message = "A 입장을 입력해주세요")
    private String positionA;

    @NotBlank(message = "B 입장을 입력해주세요")
    private String positionB;

    @Size(max = 200, message = "부가 설명은 200자 이내로 입력해주세요")
    private String description;
}
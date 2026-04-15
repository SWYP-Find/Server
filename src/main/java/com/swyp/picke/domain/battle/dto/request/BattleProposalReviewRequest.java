package com.swyp.picke.domain.battle.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class BattleProposalReviewRequest {

    @NotNull(message = "action은 필수입니다")
    private Action action;

    public enum Action {
        ACCEPT, REJECT
    }
}

package com.swyp.picke.domain.battle.dto.response;

import com.swyp.picke.domain.battle.entity.BattleProposal;
import com.swyp.picke.domain.battle.enums.BattleCategory;
import com.swyp.picke.domain.battle.enums.BattleProposalStatus;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class BattleProposalResponse {
    private final Long id;
    private final Long userId;
    private final String nickname;
    private final BattleCategory category;
    private final String topic;
    private final String positionA;
    private final String positionB;
    private final String description;
    private final BattleProposalStatus status;
    private final LocalDateTime createdAt;

    public BattleProposalResponse(BattleProposal proposal) {
        this.id = proposal.getId();
        this.userId = proposal.getUser().getId();
        this.nickname = proposal.getUser().getNickname();
        this.category = proposal.getCategory();
        this.topic = proposal.getTopic();
        this.positionA = proposal.getPositionA();
        this.positionB = proposal.getPositionB();
        this.description = proposal.getDescription();
        this.status = proposal.getStatus();
        this.createdAt = proposal.getCreatedAt();
    }
}

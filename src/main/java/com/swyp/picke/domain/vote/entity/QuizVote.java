package com.swyp.picke.domain.vote.entity;

import com.swyp.picke.domain.battle.entity.Battle;
import com.swyp.picke.domain.battle.entity.BattleOption;
import com.swyp.picke.domain.user.entity.User;
import com.swyp.picke.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "quiz_votes")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QuizVote extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "battle_id", nullable = false)
    private Battle battle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "option_id", nullable = false)
    private BattleOption selectedOption;

    @Builder
    public QuizVote(User user, Battle battle, BattleOption selectedOption) {
        this.user = user;
        this.battle = battle;
        this.selectedOption = selectedOption;
    }

    public void updateOption(BattleOption option) {
        this.selectedOption = option;
    }
}

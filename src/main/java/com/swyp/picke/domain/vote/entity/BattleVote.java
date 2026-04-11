package com.swyp.picke.domain.vote.entity;

import com.swyp.picke.domain.battle.entity.Battle;
import com.swyp.picke.domain.battle.entity.BattleOption;
import com.swyp.picke.domain.user.entity.User;
import com.swyp.picke.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "votes")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BattleVote extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "battle_id", nullable = false)
    private Battle battle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pre_vote_option_id")
    private BattleOption preVoteOption;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_vote_option_id")
    private BattleOption postVoteOption;

    @Column(name = "is_tts_listened", nullable = false)
    private Boolean isTtsListened = false;

    @Builder
    private BattleVote(User user, Battle battle, BattleOption preVoteOption,
                 BattleOption postVoteOption, Boolean isTtsListened) {
        this.user = user;
        this.battle = battle;
        this.preVoteOption = preVoteOption;
        this.postVoteOption = postVoteOption;
        this.isTtsListened = isTtsListened != null ? isTtsListened : false;
    }

    public static BattleVote createPreVote(User user, Battle battle, BattleOption option) {
        return BattleVote.builder()
                .user(user)
                .battle(battle)
                .preVoteOption(option)
                .isTtsListened(false)
                // status ?ㅼ젙 ??젣??
                .build();
    }

    public void updatePreVote(BattleOption preVoteOption) {
        this.preVoteOption = preVoteOption;
    }

    public void doPostVote(BattleOption postOption) {
        this.postVoteOption = postOption;
    }

    public void completeTts() {
        this.isTtsListened = true;
    }
}


package com.swyp.app.domain.vote.entity;

import com.swyp.app.domain.battle.entity.Battle;
import com.swyp.app.domain.battle.entity.BattleOption;
import com.swyp.app.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@Entity
@Table(name = "votes")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Vote extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // TODO: User 엔티티 병합 후 @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id") 로 교체
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "battle_id", nullable = false)
    private Battle battle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pre_vote_option_id")
    private BattleOption preVoteOption;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_vote_option_id")
    private BattleOption postVoteOption;

    @Column(name = "mind_changed", nullable = false)
    private boolean mindChanged;

    @Column(name = "reward_credits", nullable = false)
    private int rewardCredits;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private VoteStatus status;

    @Builder
    private Vote(Long userId, Battle battle, BattleOption preVoteOption,
                 BattleOption postVoteOption, boolean mindChanged, int rewardCredits, VoteStatus status) {
        this.userId = userId;
        this.battle = battle;
        this.preVoteOption = preVoteOption;
        this.postVoteOption = postVoteOption;
        this.mindChanged = mindChanged;
        this.rewardCredits = rewardCredits;
        this.status = status;
    }
}

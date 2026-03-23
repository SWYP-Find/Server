package com.swyp.app.domain.vote.entity;

import com.swyp.app.domain.battle.entity.Battle;
import com.swyp.app.domain.battle.entity.BattleOption;
import com.swyp.app.domain.vote.enums.VoteStatus;
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

@Getter
@Entity
@Table(name = "votes")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Vote extends BaseEntity {

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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private VoteStatus status;

    @Builder
    private Vote(Long userId, Battle battle, BattleOption preVoteOption,
                 BattleOption postVoteOption, VoteStatus status) {
        this.userId = userId;
        this.battle = battle;
        this.preVoteOption = preVoteOption;
        this.postVoteOption = postVoteOption;
        this.status = status;
    }

    // 사전 투표 생성 팩토리 메서드
    public static Vote createPreVote(Long userId, Battle battle, BattleOption option) {
        return Vote.builder()
                .userId(userId)
                .battle(battle)
                .preVoteOption(option)
                .status(VoteStatus.PRE_VOTED)
                .build();
    }

    // 사후 투표 실행 상태 변경 메서드
    public void doPostVote(BattleOption postOption) {
        this.postVoteOption = postOption;
        this.status = VoteStatus.POST_VOTED;
    }
}
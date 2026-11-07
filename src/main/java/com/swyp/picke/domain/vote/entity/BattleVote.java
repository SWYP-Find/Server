package com.swyp.picke.domain.vote.entity;

import com.swyp.picke.domain.battle.entity.Battle;
import com.swyp.picke.domain.battle.entity.BattleOption;
import com.swyp.picke.domain.classroom.entity.Classroom;
import com.swyp.picke.domain.user.entity.User;
import com.swyp.picke.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 배틀 투표. class_id 가 null 이면 일반 배틀, 값이 있으면 해당 클래스 안에서의 투표다.
 * 같은 유저가 같은 배틀을 일반 + 클래스별로 각각 투표할 수 있으므로 UK 는 (user, battle, class) 3 컬럼.
 * class_id null 중복까지 막는 NULLS NOT DISTINCT 인덱스는 SQL(ux_votes_user_battle_class_nn)로 관리한다.
 */
@Getter
@Entity
@Table(
        name = "votes",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_votes_user_battle_class",
                columnNames = {"user_id", "battle_id", "class_id"}
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BattleVote extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "battle_id", nullable = false)
    private Battle battle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id")
    private Classroom classroom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pre_vote_option_id")
    private BattleOption preVoteOption;

    /** 중간투표. battles.mid_vote_enabled = true 인 배틀만 사용 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mid_vote_option_id")
    private BattleOption midVoteOption;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_vote_option_id")
    private BattleOption postVoteOption;

    @Column(name = "is_tts_listened", nullable = false)
    private Boolean isTtsListened = false;

    @Builder
    private BattleVote(User user, Battle battle, Classroom classroom, BattleOption preVoteOption,
                 BattleOption midVoteOption, BattleOption postVoteOption, Boolean isTtsListened) {
        this.user = user;
        this.battle = battle;
        this.classroom = classroom;
        this.preVoteOption = preVoteOption;
        this.midVoteOption = midVoteOption;
        this.postVoteOption = postVoteOption;
        this.isTtsListened = isTtsListened != null ? isTtsListened : false;
    }

    public static BattleVote createPreVote(User user, Battle battle, BattleOption option) {
        return createPreVote(user, battle, null, option);
    }

    public static BattleVote createPreVote(User user, Battle battle, Classroom classroom, BattleOption option) {
        return BattleVote.builder()
                .user(user)
                .battle(battle)
                .classroom(classroom)
                .preVoteOption(option)
                .isTtsListened(false)
                .build();
    }

    public void updatePreVote(BattleOption preVoteOption) {
        this.preVoteOption = preVoteOption;
    }

    public void doMidVote(BattleOption midOption) {
        this.midVoteOption = midOption;
    }

    public void doPostVote(BattleOption postOption) {
        this.postVoteOption = postOption;
    }

    public void completeTts() {
        this.isTtsListened = true;
    }
}



package com.swyp.picke.domain.user.entity;

import com.swyp.picke.domain.battle.entity.Battle;
import com.swyp.picke.domain.classroom.entity.Classroom;
import com.swyp.picke.domain.user.enums.UserBattleStep;
import com.swyp.picke.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 유저별 배틀 진행 단계. class_id 가 null 이면 일반 배틀, 값이 있으면 해당 클래스 안에서의 진행이다.
 * 기존 UK(user_id, battle_id) 는 SQL 에서 drop 하고, class_id null 중복까지 막는
 * NULLS NOT DISTINCT 인덱스(ux_user_battles_user_battle_class_nn)로 대체한다.
 */
@Getter
@Entity
@Table(
        name = "user_battles",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_user_battles_user_battle_class",
                        columnNames = {"user_id", "battle_id", "class_id"}
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserBattle extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "battle_id", nullable = false)
    private Battle battle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id")
    private Classroom classroom;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserBattleStep step;

    @Builder
    public UserBattle(User user, Battle battle, Classroom classroom, UserBattleStep step) {
        this.user = user;
        this.battle = battle;
        this.classroom = classroom;
        this.step = (step != null) ? step : UserBattleStep.NONE;
    }

    public void updateStep(UserBattleStep nextStep) {
        this.step = nextStep;
    }
}
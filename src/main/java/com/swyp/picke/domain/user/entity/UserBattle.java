package com.swyp.picke.domain.user.entity;

import com.swyp.picke.domain.battle.entity.Battle;
import com.swyp.picke.domain.user.enums.UserBattleStep;
import com.swyp.picke.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "user_battles",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_user_battle",
                        columnNames = {"user_id", "battle_id"} // 유저와 배틀의 조합은 유일해야 함
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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserBattleStep step;

    @Builder
    public UserBattle(User user, Battle battle, UserBattleStep step) {
        this.user = user;
        this.battle = battle;
        this.step = (step != null) ? step : UserBattleStep.NONE;
    }

    public void updateStep(UserBattleStep nextStep) {
        this.step = nextStep;
    }
}
package com.swyp.picke.domain.battle.entity;

import com.swyp.picke.domain.battle.enums.BattleCategory;
import com.swyp.picke.domain.battle.enums.BattleProposalStatus;
import com.swyp.picke.domain.user.entity.User;
import com.swyp.picke.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "battle_proposals")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BattleProposal extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private BattleCategory category;

    @Column(nullable = false)
    private String topic;

    @Column(name = "position_a", nullable = false)
    private String positionA;

    @Column(name = "position_b", nullable = false)
    private String positionB;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BattleProposalStatus status;

    @Builder
    public BattleProposal(User user, BattleCategory category, String topic,
                          String positionA, String positionB, String description) {
        this.user = user;
        this.category = category;
        this.topic = topic;
        this.positionA = positionA;
        this.positionB = positionB;
        this.description = description;
        this.status = BattleProposalStatus.PENDING;
    }

    public void accept() {
        this.status = BattleProposalStatus.ACCEPTED;
    }

    public void reject() {
        this.status = BattleProposalStatus.REJECTED;
    }
}

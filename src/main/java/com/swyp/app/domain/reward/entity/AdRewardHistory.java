package com.swyp.app.domain.reward.entity;

import com.swyp.app.domain.reward.enums.RewardType;
import com.swyp.app.domain.user.entity.User;
import com.swyp.app.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Table(name = "ad_reward_history")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AdRewardHistory extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "transaction_id", unique = true, nullable = false)
    private String transactionId;

    @Column(name = "reward_amount", nullable = false)
    private int rewardAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "reward_type", nullable = false)
    private RewardType rewardType;

    @Builder
    public AdRewardHistory(User user, String transactionId, int rewardAmount, RewardType rewardType) {
        this.user = user;
        this.transactionId = transactionId;
        this.rewardAmount = rewardAmount;
        this.rewardType = rewardType;
    }
}
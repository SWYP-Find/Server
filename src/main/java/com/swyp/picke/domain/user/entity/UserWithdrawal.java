package com.swyp.picke.domain.user.entity;

import com.swyp.picke.domain.user.enums.WithdrawalReason;
import com.swyp.picke.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "user_withdrawals")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserWithdrawal extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason", nullable = false, length = 50)
    private WithdrawalReason reason;

    @Builder
    private UserWithdrawal(User user, WithdrawalReason reason) {
        this.user = user;
        this.reason = reason;
    }
}

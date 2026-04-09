package com.swyp.picke.domain.user.entity;

import com.swyp.picke.domain.user.enums.CreditType;
import com.swyp.picke.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "credit_histories", indexes = {
        @Index(name = "idx_credit_history_user_id", columnList = "user_id"),
        @Index(name = "idx_credit_history_user_type_ref", columnList = "user_id, credit_type, reference_id", unique = true)
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CreditHistory extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "credit_type", nullable = false, length = 30)
    private CreditType creditType;

    @Column(nullable = false)
    private int amount;

    @Column(name = "reference_id", nullable = false)
    private Long referenceId;

    @Builder
    private CreditHistory(User user, CreditType creditType, int amount, Long referenceId) {
        this.user = user;
        this.creditType = creditType;
        this.amount = amount;
        this.referenceId = referenceId;
    }
}

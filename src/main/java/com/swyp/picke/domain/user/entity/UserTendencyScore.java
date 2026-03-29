package com.swyp.picke.domain.user.entity;

import com.swyp.picke.global.common.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "user_tendency_scores")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserTendencyScore extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    private int principle;

    private int reason;

    private int individual;

    private int change;

    private int inner;

    private int ideal;

    @Builder
    private UserTendencyScore(User user, int principle, int reason, int individual,
                              int change, int inner, int ideal) {
        this.user = user;
        this.principle = principle;
        this.reason = reason;
        this.individual = individual;
        this.change = change;
        this.inner = inner;
        this.ideal = ideal;
    }

    public void update(int principle, int reason, int individual,
                       int change, int inner, int ideal) {
        this.principle = principle;
        this.reason = reason;
        this.individual = individual;
        this.change = change;
        this.inner = inner;
        this.ideal = ideal;
    }
}

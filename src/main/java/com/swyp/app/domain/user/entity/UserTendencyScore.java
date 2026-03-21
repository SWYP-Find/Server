package com.swyp.app.domain.user.entity;

import com.swyp.app.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
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

    @Id
    private Long userId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "score1")
    private int principle;

    @Column(name = "score2")
    private int reason;

    @Column(name = "score3")
    private int individual;

    @Column(name = "score4")
    private int change;

    @Column(name = "score5")
    private int inner;

    @Column(name = "score6")
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

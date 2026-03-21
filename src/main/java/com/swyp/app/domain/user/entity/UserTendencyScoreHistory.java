package com.swyp.app.domain.user.entity;

import com.swyp.app.global.common.BaseEntity;
import jakarta.persistence.Entity;
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
@Table(name = "user_tendency_score_histories")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserTendencyScoreHistory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private int principle;

    private int reason;

    private int individual;

    private int change;

    private int inner;

    private int ideal;

    @Builder
    private UserTendencyScoreHistory(User user, int principle, int reason, int individual,
                                     int change, int inner, int ideal) {
        this.user = user;
        this.principle = principle;
        this.reason = reason;
        this.individual = individual;
        this.change = change;
        this.inner = inner;
        this.ideal = ideal;
    }
}

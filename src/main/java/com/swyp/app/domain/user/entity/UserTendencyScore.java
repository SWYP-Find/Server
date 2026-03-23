package com.swyp.app.domain.user.entity;

import com.swyp.app.global.common.BaseEntity;
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

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    private int score1;
    private int score2;
    private int score3;
    private int score4;
    private int score5;
    private int score6;

    @Builder
    private UserTendencyScore(User user, int score1, int score2, int score3, int score4, int score5, int score6) {
        this.user = user;
        this.score1 = score1;
        this.score2 = score2;
        this.score3 = score3;
        this.score4 = score4;
        this.score5 = score5;
        this.score6 = score6;
    }

    public void update(int score1, int score2, int score3, int score4, int score5, int score6) {
        this.score1 = score1;
        this.score2 = score2;
        this.score3 = score3;
        this.score4 = score4;
        this.score5 = score5;
        this.score6 = score6;
    }
}

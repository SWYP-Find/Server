package com.swyp.picke.domain.poll.entity;

import com.swyp.picke.domain.user.entity.User;
import com.swyp.picke.global.common.BaseEntity;
import jakarta.persistence.Entity;
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
@Table(name = "poll_user_votes")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PollUserVote extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "poll_id", nullable = false)
    private Poll poll;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "option_id", nullable = false)
    private PollOption selectedOption;

    @Builder
    public PollUserVote(User user, Poll poll, PollOption selectedOption) {
        this.user = user;
        this.poll = poll;
        this.selectedOption = selectedOption;
    }

    public void updateOption(PollOption option) {
        this.selectedOption = option;
    }
}

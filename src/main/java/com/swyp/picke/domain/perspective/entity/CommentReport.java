package com.swyp.picke.domain.perspective.entity;

import com.swyp.picke.domain.user.entity.User;
import com.swyp.picke.global.common.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "comment_reports",
        uniqueConstraints = @UniqueConstraint(columnNames = {"comment_id", "user_id"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CommentReport extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comment_id", nullable = false)
    private PerspectiveComment comment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Builder
    private CommentReport(PerspectiveComment comment, User user) {
        this.comment = comment;
        this.user = user;
    }
}

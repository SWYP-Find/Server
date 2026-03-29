package com.swyp.picke.domain.user.entity;

import com.swyp.picke.global.common.BaseEntity;
import jakarta.persistence.Column;
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
@Table(name = "user_settings")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserSettings extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "new_battle_enabled")
    private boolean newBattleEnabled;

    @Column(name = "battle_result_enabled")
    private boolean battleResultEnabled;

    @Column(name = "comment_reply_enabled")
    private boolean commentReplyEnabled;

    @Column(name = "new_comment_enabled")
    private boolean newCommentEnabled;

    @Column(name = "content_like_enabled")
    private boolean contentLikeEnabled;

    @Column(name = "marketing_event_enabled")
    private boolean marketingEventEnabled;

    @Builder
    private UserSettings(User user, boolean newBattleEnabled, boolean battleResultEnabled,
                         boolean commentReplyEnabled, boolean newCommentEnabled,
                         boolean contentLikeEnabled, boolean marketingEventEnabled) {
        this.user = user;
        this.newBattleEnabled = newBattleEnabled;
        this.battleResultEnabled = battleResultEnabled;
        this.commentReplyEnabled = commentReplyEnabled;
        this.newCommentEnabled = newCommentEnabled;
        this.contentLikeEnabled = contentLikeEnabled;
        this.marketingEventEnabled = marketingEventEnabled;
    }

    public void update(Boolean newBattleEnabled, Boolean battleResultEnabled,
                       Boolean commentReplyEnabled, Boolean newCommentEnabled,
                       Boolean contentLikeEnabled, Boolean marketingEventEnabled) {
        if (newBattleEnabled != null) {
            this.newBattleEnabled = newBattleEnabled;
        }
        if (battleResultEnabled != null) {
            this.battleResultEnabled = battleResultEnabled;
        }
        if (commentReplyEnabled != null) {
            this.commentReplyEnabled = commentReplyEnabled;
        }
        if (newCommentEnabled != null) {
            this.newCommentEnabled = newCommentEnabled;
        }
        if (contentLikeEnabled != null) {
            this.contentLikeEnabled = contentLikeEnabled;
        }
        if (marketingEventEnabled != null) {
            this.marketingEventEnabled = marketingEventEnabled;
        }
    }
}

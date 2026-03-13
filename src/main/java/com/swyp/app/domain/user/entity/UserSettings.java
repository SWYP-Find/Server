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
@Table(name = "user_settings")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserSettings extends BaseEntity {

    @Id
    private Long userId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    private boolean pushEnabled;

    private boolean emailEnabled;

    private boolean debateRequestEnabled;

    private boolean profilePublic;

    @Builder
    private UserSettings(User user, boolean pushEnabled, boolean emailEnabled, boolean debateRequestEnabled, boolean profilePublic) {
        this.user = user;
        this.pushEnabled = pushEnabled;
        this.emailEnabled = emailEnabled;
        this.debateRequestEnabled = debateRequestEnabled;
        this.profilePublic = profilePublic;
    }

    public void update(Boolean pushEnabled, Boolean emailEnabled, Boolean debateRequestEnabled, Boolean profilePublic) {
        if (pushEnabled != null) {
            this.pushEnabled = pushEnabled;
        }
        if (emailEnabled != null) {
            this.emailEnabled = emailEnabled;
        }
        if (debateRequestEnabled != null) {
            this.debateRequestEnabled = debateRequestEnabled;
        }
        if (profilePublic != null) {
            this.profilePublic = profilePublic;
        }
    }
}

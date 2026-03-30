package com.swyp.picke.domain.user.entity;

import com.swyp.picke.domain.user.enums.AgreementType;
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

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "user_agreements")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserAgreement extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "agreement_type", nullable = false, length = 50)
    private AgreementType agreementType;

    @Column(nullable = false, length = 20)
    private String version;

    @Column(name = "agreed_at", nullable = false)
    private LocalDateTime agreedAt;

    @Builder
    private UserAgreement(User user, AgreementType agreementType, String version, LocalDateTime agreedAt) {
        this.user = user;
        this.agreementType = agreementType;
        this.version = version;
        this.agreedAt = agreedAt;
    }
}

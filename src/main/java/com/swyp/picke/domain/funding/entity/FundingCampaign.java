package com.swyp.picke.domain.funding.entity;

import com.swyp.picke.domain.funding.enums.FundingCampaignStatus;
import com.swyp.picke.domain.funding.enums.TumblbugPlan;
import com.swyp.picke.domain.ticket.enums.FundingPlatform;
import com.swyp.picke.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 외부 플랫폼에서 진행하는 펀딩 캠페인을 저장합니다.
@Entity
@Table(name = "funding_campaigns",
        uniqueConstraints = @UniqueConstraint(name = "uk_funding_campaign_platform_external",
                columnNames = {"platform", "external_campaign_id"}),
        indexes = @Index(name = "idx_funding_campaign_status", columnList = "status"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FundingCampaign extends BaseEntity {

    // 펀딩 플랫폼
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FundingPlatform platform;

    // 플랫폼의 캠페인 식별자
    @Column(name = "external_campaign_id", nullable = false, length = 120)
    private String externalCampaignId;

    // 캠페인 표시 이름
    @Column(nullable = false, length = 120)
    private String name;

    // 캠페인 운영 상태
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FundingCampaignStatus status = FundingCampaignStatus.ACTIVE;

    // 티켓 등록 정책 버전
    @Column(name = "register_policy_version", nullable = false, length = 30)
    private String registerPolicyVersion = "1";

    @Enumerated(EnumType.STRING)
    @Column(name = "tumblbug_plan", nullable = false, length = 20)
    private TumblbugPlan tumblbugPlan = TumblbugPlan.RUN;

    public FundingCampaign(FundingPlatform platform, String externalCampaignId, String name) {
        this(platform, externalCampaignId, name, TumblbugPlan.RUN);
    }

    public FundingCampaign(FundingPlatform platform, String externalCampaignId, String name, TumblbugPlan tumblbugPlan) {
        this.platform = platform;
        this.externalCampaignId = externalCampaignId;
        this.name = name;
        this.tumblbugPlan = tumblbugPlan;
    }
}

package com.swyp.picke.domain.funding.entity;

import com.swyp.picke.domain.ticket.entity.FundingOrder;
import com.swyp.picke.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

/** Original XLSX row and external identity only. Payment/ticket state lives in domain.ticket. */
@Entity
@Table(name = "tumblbug_import_records", uniqueConstraints = {
        @UniqueConstraint(name = "uk_tumblbug_supporter_line",
                columnNames = {"campaign_id", "supporter_number", "reward_line_id"}),
        @UniqueConstraint(name = "uk_tumblbug_funding_order", columnNames = "funding_order_id")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TumblbugImportRecord extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "campaign_id", nullable = false)
    private FundingCampaign campaign;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "funding_order_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private FundingOrder fundingOrder;

    @Column(name = "supporter_number", nullable = false, length = 120)
    private String supporterNumber;

    @Column(name = "reward_line_id", nullable = false, length = 120)
    private String rewardLineId;

    @Column(name = "source_version", length = 50)
    private String sourceVersion;

    @Column(name = "source_snapshot", nullable = false, columnDefinition = "TEXT")
    private String sourceSnapshot;

    @Column(name = "delivery_recorded_ticket_id")
    private Long deliveryRecordedTicketId;

    public TumblbugImportRecord(FundingCampaign campaign, FundingOrder fundingOrder, String supporterNumber,
                               String rewardLineId, String sourceVersion, String sourceSnapshot) {
        this.campaign = campaign;
        this.fundingOrder = fundingOrder;
        this.supporterNumber = supporterNumber;
        this.rewardLineId = rewardLineId;
        this.sourceVersion = sourceVersion;
        this.sourceSnapshot = sourceSnapshot;
    }

    public void recordDelivery(Long ticketId) {
        this.deliveryRecordedTicketId = ticketId;
    }
}

package com.swyp.picke.domain.ticket.entity;

import com.swyp.picke.domain.ticket.enums.FundingPlatform;
import com.swyp.picke.domain.ticket.enums.PaymentStatus;
import com.swyp.picke.domain.ticket.enums.RewardTier;
import com.swyp.picke.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 펀딩 플랫폼 결제 명단 원본. 어드민이 CSV 로 업로드하며 order_id 기준으로 멱등하다.
 * 구매자(supporter)와 이용권 등록자(ClassTicket.registeredUser)는 다를 수 있다.
 */
@Getter
@Entity
@Table(
        name = "funding_orders",
        uniqueConstraints = @UniqueConstraint(name = "uk_funding_orders_order_id", columnNames = "order_id")
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FundingOrder extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FundingPlatform platform;

    @Column(name = "campaign_id", nullable = false, length = 50)
    private String campaignId;

    @Column(name = "order_id", nullable = false, length = 50)
    private String orderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "reward_tier", nullable = false, length = 10)
    private RewardTier rewardTier;

    /** 1 이 아니면 발급 보류 (PRD §30) */
    @Column(nullable = false)
    private int quantity;

    @Column(name = "supporter_name", nullable = false, length = 50)
    private String supporterName;

    @Column(name = "supporter_email")
    private String supporterEmail;

    @Column(name = "supporter_phone", length = 20)
    private String supporterPhone;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 20)
    private PaymentStatus paymentStatus;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "import_file_hash", length = 64)
    private String importFileHash;

    @Builder
    private FundingOrder(FundingPlatform platform, String campaignId, String orderId, RewardTier rewardTier,
                         int quantity, String supporterName, String supporterEmail, String supporterPhone,
                         PaymentStatus paymentStatus, LocalDateTime paidAt, String importFileHash) {
        this.platform = platform;
        this.campaignId = campaignId;
        this.orderId = orderId;
        this.rewardTier = rewardTier;
        this.quantity = quantity;
        this.supporterName = supporterName;
        this.supporterEmail = supporterEmail;
        this.supporterPhone = supporterPhone;
        this.paymentStatus = paymentStatus;
        this.paidAt = paidAt;
        this.importFileHash = importFileHash;
    }

    /** 환불·취소는 종착 상태. 오래된 결제 확정 명단을 다시 올려도 되살리지 않는다 (CF07) */
    public boolean isTerminal() {
        return paymentStatus == PaymentStatus.REFUNDED || paymentStatus == PaymentStatus.CANCELED;
    }

    /**
     * 명단 재업로드 반영. 바뀐 값이 있으면 true. 상태 전이 검증(종착 상태·리워드 변경)은 호출 측에서 끝낸 뒤 부른다.
     * import_file_hash 는 변경 여부와 무관하게 마지막 업로드 파일로 갱신한다.
     */
    public boolean applyImport(FundingPlatform platform, String campaignId, RewardTier rewardTier, int quantity,
                               String supporterName, String supporterEmail, String supporterPhone,
                               PaymentStatus paymentStatus, LocalDateTime paidAt, String importFileHash) {
        boolean changed = this.platform != platform
                || !Objects.equals(this.campaignId, campaignId)
                || this.rewardTier != rewardTier
                || this.quantity != quantity
                || !Objects.equals(this.supporterName, supporterName)
                || !Objects.equals(this.supporterEmail, supporterEmail)
                || !Objects.equals(this.supporterPhone, supporterPhone)
                || this.paymentStatus != paymentStatus
                || !Objects.equals(this.paidAt, paidAt);
        this.platform = platform;
        this.campaignId = campaignId;
        this.rewardTier = rewardTier;
        this.quantity = quantity;
        this.supporterName = supporterName;
        this.supporterEmail = supporterEmail;
        this.supporterPhone = supporterPhone;
        this.paymentStatus = paymentStatus;
        this.paidAt = paidAt;
        this.importFileHash = importFileHash;
        return changed;
    }

    public void updatePayment(PaymentStatus paymentStatus, LocalDateTime paidAt) {
        this.paymentStatus = paymentStatus;
        this.paidAt = paidAt;
    }

    public boolean isIssuable() {
        return paymentStatus == PaymentStatus.PAID_CONFIRMED && paidAt != null && quantity > 0;
    }
}

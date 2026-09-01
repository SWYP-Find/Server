package com.swyp.picke.domain.ad.entity;

import com.swyp.picke.domain.ad.enums.AdNetwork;
import com.swyp.picke.domain.ad.enums.AdSlotCode;
import com.swyp.picke.domain.ad.enums.AdStatus;
import com.swyp.picke.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 제휴 광고 소재. 각 매체 콘솔에서 발급한 완성형 제휴 링크를 어드민이 등록한다.
 */
@Entity
@Getter
@Table(name = "ad_creatives")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AdCreative extends BaseEntity {

    /** 공개 클릭 URL(/c/{code})에 노출되는 짧은 코드. PK를 그대로 드러내지 않기 위해 둔다. */
    @Column(name = "code", nullable = false, unique = true, length = 16)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "network", nullable = false, length = 20)
    private AdNetwork network;

    @Enumerated(EnumType.STRING)
    @Column(name = "slot", nullable = false, length = 40)
    private AdSlotCode slot;

    @Column(name = "title", nullable = false, length = 100)
    private String title;

    @Column(name = "subtitle", length = 200)
    private String subtitle;

    @Column(name = "image_url", nullable = false, length = 500)
    private String imageUrl;

    /** 쿠팡은 "구매하러 가기", 애드픽 CPI는 "설치하고 받기" 식으로 성격이 달라 소재 단위로 둔다. */
    @Column(name = "cta_text", nullable = false, length = 30)
    private String ctaText;

    @Column(name = "landing_url", nullable = false, length = 1000)
    private String landingUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AdStatus status;

    @Column(name = "weight", nullable = false)
    private int weight;

    @Column(name = "starts_at")
    private LocalDateTime startsAt;

    @Column(name = "ends_at")
    private LocalDateTime endsAt;

    @Builder
    private AdCreative(String code, AdNetwork network, AdSlotCode slot, String title, String subtitle,
                       String imageUrl, String ctaText, String landingUrl, AdStatus status, Integer weight,
                       LocalDateTime startsAt, LocalDateTime endsAt) {
        this.code = code;
        this.network = network;
        this.slot = slot;
        this.title = title;
        this.subtitle = subtitle;
        this.imageUrl = imageUrl;
        this.ctaText = ctaText;
        this.landingUrl = landingUrl;
        this.status = status != null ? status : AdStatus.DRAFT;
        this.weight = weight != null ? weight : 1;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
    }

    public void update(AdNetwork network, AdSlotCode slot, String title, String subtitle, String imageUrl,
                       String ctaText, String landingUrl, AdStatus status, Integer weight,
                       LocalDateTime startsAt, LocalDateTime endsAt) {
        this.network = network;
        this.slot = slot;
        this.title = title;
        this.subtitle = subtitle;
        this.imageUrl = imageUrl;
        this.ctaText = ctaText;
        this.landingUrl = landingUrl;
        this.status = status;
        this.weight = weight != null ? weight : 1;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
    }

    /** 게재 가능 여부. status와 기간을 함께 본다. */
    public boolean isServable(LocalDateTime now) {
        if (status != AdStatus.ACTIVE) {
            return false;
        }
        if (startsAt != null && now.isBefore(startsAt)) {
            return false;
        }
        return endsAt == null || !now.isAfter(endsAt);
    }
}

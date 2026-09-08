package com.swyp.picke.domain.ad.entity;

import com.swyp.picke.domain.ad.enums.AdNetwork;
import com.swyp.picke.domain.ad.enums.AdSlotCode;
import com.swyp.picke.domain.ad.enums.AdSource;
import com.swyp.picke.domain.ad.enums.AdStatus;
import com.swyp.picke.domain.ad.enums.AdTargetOs;
import com.swyp.picke.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.Check;
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
@Table(name = "ad_creatives", uniqueConstraints = {
        @UniqueConstraint(name = "uk_ad_creatives_external", columnNames = {"source", "external_id"})
})
@Check(name = "ck_ad_creatives_network", constraints = "network in ('COUPANG', 'ADPICK')")
@Check(name = "ck_ad_creatives_slot", constraints = "slot in ('HOME_FEED', 'BATTLE_RESULT_BOTTOM', "
        + "'CHAT_ROOM_INLINE', 'ATTENDANCE_COMPLETE', 'PROFILE_BOTTOM')")
@Check(name = "ck_ad_creatives_status", constraints = "status in ('DRAFT', 'ACTIVE', 'PAUSED')")
@Check(name = "ck_ad_creatives_source", constraints = "source in ('MANUAL', 'ADPICK_API')")
@Check(name = "ck_ad_creatives_target_os", constraints = "target_os in ('ALL', 'ANDROID', 'IOS')")
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

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 20)
    private AdSource source;

    /** 매체 쪽 캠페인 식별자. 동기화 소재를 다시 찾을 때 쓴다. 수동 등록 소재는 비어 있다. */
    @Column(name = "external_id", length = 64)
    private String externalId;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_os", nullable = false, length = 20)
    private AdTargetOs targetOs;

    @Column(name = "weight", nullable = false)
    private int weight;

    @Column(name = "starts_at")
    private LocalDateTime startsAt;

    @Column(name = "ends_at")
    private LocalDateTime endsAt;

    @Builder
    private AdCreative(String code, AdNetwork network, AdSlotCode slot, String title, String subtitle,
                       String imageUrl, String ctaText, String landingUrl, AdStatus status, Integer weight,
                       LocalDateTime startsAt, LocalDateTime endsAt, AdSource source, String externalId,
                       AdTargetOs targetOs) {
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
        this.source = source != null ? source : AdSource.MANUAL;
        this.externalId = externalId;
        this.targetOs = targetOs != null ? targetOs : AdTargetOs.ALL;
    }

    public void update(AdNetwork network, AdSlotCode slot, String title, String subtitle, String imageUrl,
                       String ctaText, String landingUrl, AdStatus status, Integer weight,
                       LocalDateTime startsAt, LocalDateTime endsAt, AdTargetOs targetOs) {
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
        this.targetOs = targetOs != null ? targetOs : AdTargetOs.ALL;
    }

    /**
     * 애드픽 동기화가 매 회차 내용을 덮어쓴다.
     * 어드민이 꺼둔 소재는 다시 켜지 않는다. 별도 플래그 없이 PAUSED 를 끄기 스위치로 쓴다.
     */
    public void syncFromAdpick(String title, String subtitle, String imageUrl, String ctaText,
                               String landingUrl, AdTargetOs targetOs, AdSlotCode slot, boolean servable) {
        this.title = title;
        this.subtitle = subtitle;
        this.imageUrl = imageUrl;
        this.ctaText = ctaText;
        this.landingUrl = landingUrl;
        this.targetOs = targetOs;
        this.slot = slot;
        if (this.status != AdStatus.PAUSED) {
            this.status = servable ? AdStatus.ACTIVE : AdStatus.DRAFT;
        }
    }

    /** 동기화가 소유하는 소재는 어드민이 내용을 고치거나 지우지 않는다. */
    public boolean isManaged() {
        return source == AdSource.ADPICK_API;
    }

    public void changeStatus(AdStatus status) {
        this.status = status;
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

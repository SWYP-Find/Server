package com.swyp.picke.domain.ad.entity;

import com.swyp.picke.domain.ad.enums.AdSlotCode;
import com.swyp.picke.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 일별 노출 집계.
 *
 * <p>노출을 raw 로그로 쌓으면 배너가 스크롤에 걸릴 때마다 행이 생겨 금방 수천만 건이 된다.
 * CTR 산출에는 일별 카운터로 충분하다.
 */
@Entity
@Getter
@Table(name = "ad_impression_daily", uniqueConstraints = {
        @UniqueConstraint(name = "uk_ad_impression_daily", columnNames = {"creative_id", "slot", "stat_date"})
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AdImpressionDaily extends BaseEntity {

    @Column(name = "creative_id", nullable = false)
    private Long creativeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "slot", nullable = false, length = 40)
    private AdSlotCode slot;

    @Column(name = "stat_date", nullable = false)
    private LocalDate statDate;

    @Column(name = "impressions", nullable = false)
    private long impressions;

    @Builder
    private AdImpressionDaily(Long creativeId, AdSlotCode slot, LocalDate statDate, long impressions) {
        this.creativeId = creativeId;
        this.slot = slot;
        this.statDate = statDate;
        this.impressions = impressions;
    }
}

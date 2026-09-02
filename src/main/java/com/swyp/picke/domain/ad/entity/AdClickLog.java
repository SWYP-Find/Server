package com.swyp.picke.domain.ad.entity;

import com.swyp.picke.domain.ad.enums.AdSlotCode;
import com.swyp.picke.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import org.hibernate.annotations.Check;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 클릭 원장. 제휴사 리포트와 대조하는 용도다.
 * 클릭 시각은 {@code BaseEntity.createdAt}이다. /c/{code}는 외부 브라우저에서 열려
 * Authorization 헤더가 없으므로 사용자를 특정하지 않는다.
 */
@Entity
@Getter
@Table(name = "ad_click_logs", indexes = {
        @Index(name = "idx_ad_click_logs_creative", columnList = "creative_id")
})
@Check(name = "ck_ad_click_logs_slot", constraints = "slot in ('HOME_FEED', 'BATTLE_RESULT_BOTTOM', "
        + "'CHAT_ROOM_INLINE', 'ATTENDANCE_COMPLETE', 'PROFILE_BOTTOM')")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AdClickLog extends BaseEntity {

    @Column(name = "creative_id", nullable = false)
    private Long creativeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "slot", nullable = false, length = 40)
    private AdSlotCode slot;

    /** 원본 IP는 저장하지 않는다. 중복 클릭 판별에 필요한 정도만 남긴다. */
    @Column(name = "ip_hash", length = 64)
    private String ipHash;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Builder
    private AdClickLog(Long creativeId, AdSlotCode slot, String ipHash, String userAgent) {
        this.creativeId = creativeId;
        this.slot = slot;
        this.ipHash = ipHash;
        this.userAgent = userAgent;
    }
}

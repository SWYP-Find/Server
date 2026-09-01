package com.swyp.picke.domain.admin.dto.ad.response;

import com.swyp.picke.domain.ad.entity.AdCreative;
import com.swyp.picke.domain.ad.enums.AdNetwork;
import com.swyp.picke.domain.ad.enums.AdSlotCode;
import com.swyp.picke.domain.ad.enums.AdStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "제휴 광고 소재")
public record AdCreativeResponse(
        Long id,
        String code,
        AdNetwork network,
        AdSlotCode slot,
        String title,
        String subtitle,
        String imageUrl,
        String ctaText,
        String landingUrl,
        AdStatus status,
        int weight,
        LocalDateTime startsAt,
        LocalDateTime endsAt
) {

    public static AdCreativeResponse from(AdCreative creative) {
        return new AdCreativeResponse(
                creative.getId(),
                creative.getCode(),
                creative.getNetwork(),
                creative.getSlot(),
                creative.getTitle(),
                creative.getSubtitle(),
                creative.getImageUrl(),
                creative.getCtaText(),
                creative.getLandingUrl(),
                creative.getStatus(),
                creative.getWeight(),
                creative.getStartsAt(),
                creative.getEndsAt()
        );
    }
}

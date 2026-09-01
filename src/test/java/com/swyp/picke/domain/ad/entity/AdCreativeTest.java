package com.swyp.picke.domain.ad.entity;

import com.swyp.picke.domain.ad.enums.AdNetwork;
import com.swyp.picke.domain.ad.enums.AdSlotCode;
import com.swyp.picke.domain.ad.enums.AdStatus;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AdCreativeTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 1, 12, 0);

    private AdCreative creative(AdStatus status, LocalDateTime startsAt, LocalDateTime endsAt) {
        return AdCreative.builder()
                .code("abc12345")
                .network(AdNetwork.COUPANG)
                .slot(AdSlotCode.HOME_FEED)
                .title("무선 이어폰")
                .imageUrl("https://img.example.com/1.jpg")
                .ctaText("구매하러 가기")
                .landingUrl("https://link.coupang.com/a/abcdef")
                .status(status)
                .weight(1)
                .startsAt(startsAt)
                .endsAt(endsAt)
                .build();
    }

    @Test
    @DisplayName("ACTIVE이고 기간이 열려 있으면 게재한다")
    void isServable_true() {
        assertThat(creative(AdStatus.ACTIVE, null, null).isServable(NOW)).isTrue();
    }

    @Test
    @DisplayName("PAUSED와 DRAFT는 기간과 무관하게 게재하지 않는다")
    void isServable_falseWhenNotActive() {
        assertThat(creative(AdStatus.PAUSED, null, null).isServable(NOW)).isFalse();
        assertThat(creative(AdStatus.DRAFT, null, null).isServable(NOW)).isFalse();
    }

    @Test
    @DisplayName("시작 전 소재는 게재하지 않는다")
    void isServable_falseBeforeStart() {
        assertThat(creative(AdStatus.ACTIVE, NOW.plusDays(1), null).isServable(NOW)).isFalse();
    }

    @Test
    @DisplayName("종료된 소재는 게재하지 않는다")
    void isServable_falseAfterEnd() {
        assertThat(creative(AdStatus.ACTIVE, null, NOW.minusSeconds(1)).isServable(NOW)).isFalse();
    }

    @Test
    @DisplayName("종료 시각과 정확히 같은 순간까지는 게재한다")
    void isServable_trueAtExactEnd() {
        assertThat(creative(AdStatus.ACTIVE, null, NOW).isServable(NOW)).isTrue();
    }
}

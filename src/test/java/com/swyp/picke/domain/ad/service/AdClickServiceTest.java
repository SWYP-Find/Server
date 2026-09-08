package com.swyp.picke.domain.ad.service;

import com.swyp.picke.domain.ad.entity.AdCreative;
import com.swyp.picke.domain.ad.enums.AdNetwork;
import com.swyp.picke.domain.ad.enums.AdSlotCode;
import com.swyp.picke.domain.ad.enums.AdStatus;
import com.swyp.picke.domain.ad.link.AffiliateLinkResolver;
import com.swyp.picke.domain.ad.link.CoupangLinkBuilder;
import com.swyp.picke.domain.ad.repository.AdClickLogRepository;
import com.swyp.picke.domain.ad.repository.AdCreativeRepository;
import com.swyp.picke.domain.ad.service.AdClickService.AdClickTarget;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdClickServiceTest {

    @Mock
    private AdCreativeRepository adCreativeRepository;
    @Mock
    private AdClickLogRepository adClickLogRepository;

    private AdClickService adClickService() {
        return new AdClickService(
                adCreativeRepository,
                adClickLogRepository,
                new AffiliateLinkResolver(List.of(new CoupangLinkBuilder())));
    }

    private AdCreative creative(AdStatus status, LocalDateTime endsAt) {
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
                .endsAt(endsAt)
                .build();
    }

    @Test
    @DisplayName("없는 코드는 이동 대상을 주지 않는다")
    void resolveTarget_emptyWhenCodeMissing() {
        when(adCreativeRepository.findByCode("nope0000")).thenReturn(Optional.empty());

        assertThat(adClickService().resolveTarget("nope0000")).isEmpty();
    }

    @Test
    @DisplayName("게재가 끝난 소재는 이동 대상을 주지 않는다")
    void resolveTarget_emptyWhenExpired() {
        when(adCreativeRepository.findByCode("abc12345"))
                .thenReturn(Optional.of(creative(AdStatus.ACTIVE, LocalDateTime.now().minusDays(1))));

        assertThat(adClickService().resolveTarget("abc12345")).isEmpty();
    }

    @Test
    @DisplayName("게재 중인 소재는 매체 규칙이 적용된 최종 URL을 준다")
    void resolveTarget_returnsResolvedUrl() {
        when(adCreativeRepository.findByCode("abc12345"))
                .thenReturn(Optional.of(creative(AdStatus.ACTIVE, null)));

        AdClickTarget target = adClickService().resolveTarget("abc12345").orElseThrow();

        assertThat(target.slot()).isEqualTo(AdSlotCode.HOME_FEED);
        assertThat(target.redirectUrl()).contains("subId=HOME_FEED_abc12345");
    }
}

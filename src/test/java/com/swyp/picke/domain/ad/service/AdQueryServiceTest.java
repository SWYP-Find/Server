package com.swyp.picke.domain.ad.service;

import com.swyp.picke.domain.ad.dto.response.AdResponse;
import com.swyp.picke.domain.ad.entity.AdCreative;
import com.swyp.picke.domain.ad.enums.AdNetwork;
import com.swyp.picke.domain.ad.enums.AdSlotCode;
import com.swyp.picke.domain.ad.enums.AdStatus;
import com.swyp.picke.domain.ad.repository.AdCreativeRepository;
import com.swyp.picke.domain.ad.repository.AdImpressionDailyRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdQueryServiceTest {

    @Mock
    private AdCreativeRepository adCreativeRepository;
    @Mock
    private AdImpressionDailyRepository adImpressionDailyRepository;

    @InjectMocks
    private AdQueryService adQueryService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(adQueryService, "adBaseUrl", "https://ad.picke.store");
    }

    private AdCreative creative(String code, AdStatus status, int weight,
                                LocalDateTime startsAt, LocalDateTime endsAt) {
        return AdCreative.builder()
                .code(code)
                .network(AdNetwork.COUPANG)
                .slot(AdSlotCode.HOME_FEED)
                .title("무선 이어폰")
                .imageUrl("https://img.example.com/1.jpg")
                .ctaText("구매하러 가기")
                .landingUrl("https://link.coupang.com/a/" + code)
                .status(status)
                .weight(weight)
                .startsAt(startsAt)
                .endsAt(endsAt)
                .build();
    }

    @Test
    @DisplayName("게재 기간이 지난 소재는 응답에서 제외한다")
    void findServableAds_excludesExpired() {
        AdCreative live = creative("live0001", AdStatus.ACTIVE, 1, null, null);
        AdCreative expired = creative("dead0001", AdStatus.ACTIVE, 1, null, LocalDateTime.now().minusDays(1));
        when(adCreativeRepository.findAllBySlotAndStatus(AdSlotCode.HOME_FEED, AdStatus.ACTIVE))
                .thenReturn(List.of(live, expired));

        List<AdResponse> result = adQueryService.findServableAds(AdSlotCode.HOME_FEED, 5);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).code()).isEqualTo("live0001");
    }

    @Test
    @DisplayName("게재 가능한 소재가 없으면 빈 목록을 준다. 광고 없음은 오류가 아니다")
    void findServableAds_returnsEmpty() {
        when(adCreativeRepository.findAllBySlotAndStatus(AdSlotCode.HOME_FEED, AdStatus.ACTIVE))
                .thenReturn(List.of());

        assertThat(adQueryService.findServableAds(AdSlotCode.HOME_FEED, 1)).isEmpty();
    }

    @Test
    @DisplayName("clickUrl은 광고 도메인의 짧은 코드 경로로 만든다")
    void findServableAds_buildsClickUrl() {
        when(adCreativeRepository.findAllBySlotAndStatus(AdSlotCode.HOME_FEED, AdStatus.ACTIVE))
                .thenReturn(List.of(creative("abc12345", AdStatus.ACTIVE, 1, null, null)));

        AdResponse response = adQueryService.findServableAds(AdSlotCode.HOME_FEED, 1).get(0);

        assertThat(response.clickUrl()).isEqualTo("https://ad.picke.store/c/abc12345");
        assertThat(response.label()).isEqualTo("광고");
    }

    @Test
    @DisplayName("요청 개수만큼만 주고 같은 소재를 두 번 담지 않는다")
    void findServableAds_limitsSizeWithoutDuplicates() {
        when(adCreativeRepository.findAllBySlotAndStatus(AdSlotCode.HOME_FEED, AdStatus.ACTIVE))
                .thenReturn(List.of(
                        creative("aaaa1111", AdStatus.ACTIVE, 1, null, null),
                        creative("bbbb2222", AdStatus.ACTIVE, 1, null, null),
                        creative("cccc3333", AdStatus.ACTIVE, 1, null, null)));

        List<AdResponse> result = adQueryService.findServableAds(AdSlotCode.HOME_FEED, 2);

        assertThat(result).hasSize(2);
        assertThat(result.stream().map(AdResponse::code).distinct()).hasSize(2);
    }

    @Test
    @DisplayName("가중치가 큰 소재가 확연히 자주 뽑힌다")
    void findServableAds_weightedRotation() {
        when(adCreativeRepository.findAllBySlotAndStatus(AdSlotCode.HOME_FEED, AdStatus.ACTIVE))
                .thenReturn(List.of(
                        creative("heavy001", AdStatus.ACTIVE, 99, null, null),
                        creative("light001", AdStatus.ACTIVE, 1, null, null)));

        long heavyPicks = java.util.stream.IntStream.range(0, 500)
                .mapToObj(i -> adQueryService.findServableAds(AdSlotCode.HOME_FEED, 1).get(0).code())
                .filter("heavy001"::equals)
                .count();

        assertThat(heavyPicks).isGreaterThan(400);
    }

    @Test
    @DisplayName("노출 집계는 같은 날 반복 호출하면 기존 행을 누적한다")
    void recordImpressions_incrementsExistingRow() {
        AdCreative creative = creative("abc12345", AdStatus.ACTIVE, 1, null, null);
        ReflectionTestUtils.setField(creative, "id", 7L);
        when(adCreativeRepository.findAllByCodeIn(List.of("abc12345"))).thenReturn(List.of(creative));
        when(adImpressionDailyRepository.increment(eq(7L), eq(AdSlotCode.HOME_FEED), any(LocalDate.class), anyLong()))
                .thenReturn(1);

        adQueryService.recordImpressions(List.of("abc12345"));

        verify(adImpressionDailyRepository, never()).save(any());
    }

    @Test
    @DisplayName("그날 첫 노출이면 집계 행을 새로 만든다")
    void recordImpressions_insertsWhenAbsent() {
        AdCreative creative = creative("abc12345", AdStatus.ACTIVE, 1, null, null);
        ReflectionTestUtils.setField(creative, "id", 7L);
        when(adCreativeRepository.findAllByCodeIn(List.of("abc12345"))).thenReturn(List.of(creative));
        when(adImpressionDailyRepository.increment(eq(7L), eq(AdSlotCode.HOME_FEED), any(LocalDate.class), anyLong()))
                .thenReturn(0);

        adQueryService.recordImpressions(List.of("abc12345"));

        verify(adImpressionDailyRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("동시에 같은 집계 행을 만들면 갱신으로 되돌린다")
    void recordImpressions_retriesOnConcurrentInsert() {
        AdCreative creative = creative("abc12345", AdStatus.ACTIVE, 1, null, null);
        ReflectionTestUtils.setField(creative, "id", 7L);
        when(adCreativeRepository.findAllByCodeIn(List.of("abc12345"))).thenReturn(List.of(creative));
        when(adImpressionDailyRepository.increment(eq(7L), eq(AdSlotCode.HOME_FEED), any(LocalDate.class), anyLong()))
                .thenReturn(0, 1);
        when(adImpressionDailyRepository.save(any())).thenThrow(new DataIntegrityViolationException("duplicate"));

        adQueryService.recordImpressions(List.of("abc12345"));

        verify(adImpressionDailyRepository, times(2))
                .increment(eq(7L), eq(AdSlotCode.HOME_FEED), any(LocalDate.class), anyLong());
    }
}

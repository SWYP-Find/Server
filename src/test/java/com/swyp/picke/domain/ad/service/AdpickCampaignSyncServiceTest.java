package com.swyp.picke.domain.ad.service;

import com.swyp.picke.domain.ad.client.AdpickCampaignClient;
import com.swyp.picke.domain.ad.client.AdpickCampaignResponse;
import com.swyp.picke.domain.ad.entity.AdCreative;
import com.swyp.picke.domain.ad.enums.AdNetwork;
import com.swyp.picke.domain.ad.enums.AdSlotCode;
import com.swyp.picke.domain.ad.enums.AdSource;
import com.swyp.picke.domain.ad.enums.AdStatus;
import com.swyp.picke.domain.ad.enums.AdTargetOs;
import com.swyp.picke.domain.ad.repository.AdCreativeRepository;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AdpickCampaignSyncServiceTest {

    private static final String CTA = "설치하고 받기";

    @Mock
    private AdpickCampaignClient adpickCampaignClient;
    @Mock
    private AdCreativeRepository adCreativeRepository;
    @Mock
    private AdCreativeCodeGenerator adCreativeCodeGenerator;

    @InjectMocks
    private AdpickCampaignSyncService syncService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(syncService, "slot", AdSlotCode.BATTLE_RESULT_BOTTOM);
        ReflectionTestUtils.setField(syncService, "ctaText", CTA);
        when(adpickCampaignClient.isConfigured()).thenReturn(true);
        when(adCreativeCodeGenerator.generate()).thenReturn("syn23456");
        when(adCreativeRepository.findAllBySource(AdSource.ADPICK_API)).thenReturn(List.of());
    }

    private AdpickCampaignResponse campaign(String offerId, String os, Integer remain) {
        return new AdpickCampaignResponse(
                offerId,
                "리워디 월렛 - 금테크 지갑",
                "금 모으기 앱테크",
                "설명 텍스트",
                Map.of("icon", "https://play-lh.googleusercontent.com/icon.png"),
                "https://deg.kr/39e859f",
                os,
                remain
        );
    }

    private AdCreative managed(String externalId, AdStatus status) {
        return AdCreative.builder()
                .code("syn00001")
                .network(AdNetwork.ADPICK)
                .slot(AdSlotCode.BATTLE_RESULT_BOTTOM)
                .title("예전 제목")
                .imageUrl("https://img.example.com/old.png")
                .ctaText(CTA)
                .landingUrl("https://deg.kr/old")
                .status(status)
                .weight(1)
                .source(AdSource.ADPICK_API)
                .externalId(externalId)
                .targetOs(AdTargetOs.ANDROID)
                .build();
    }

    @Test
    @DisplayName("affId가 없으면 호출하지 않고 건너뛴다")
    void sync_skipsWhenNotConfigured() {
        when(adpickCampaignClient.isConfigured()).thenReturn(false);

        assertThat(syncService.sync()).isZero();
        verify(adpickCampaignClient, never()).fetchCampaigns();
    }

    @Test
    @DisplayName("새 캠페인을 소재로 만들면서 매체·지면·OS를 채운다")
    void sync_createsCreativeFromCampaign() {
        when(adpickCampaignClient.fetchCampaigns()).thenReturn(List.of(campaign("16b04", "Android", 120)));

        syncService.sync();

        ArgumentCaptor<AdCreative> captor = ArgumentCaptor.forClass(AdCreative.class);
        verify(adCreativeRepository).save(captor.capture());
        AdCreative saved = captor.getValue();

        assertThat(saved.getNetwork()).isEqualTo(AdNetwork.ADPICK);
        assertThat(saved.getSource()).isEqualTo(AdSource.ADPICK_API);
        assertThat(saved.getExternalId()).isEqualTo("16b04");
        assertThat(saved.getSlot()).isEqualTo(AdSlotCode.BATTLE_RESULT_BOTTOM);
        assertThat(saved.getTargetOs()).isEqualTo(AdTargetOs.ANDROID);
        assertThat(saved.getStatus()).isEqualTo(AdStatus.ACTIVE);
        assertThat(saved.getLandingUrl()).isEqualTo("https://deg.kr/39e859f");
        assertThat(saved.getImageUrl()).isEqualTo("https://play-lh.googleusercontent.com/icon.png");
        assertThat(saved.getCtaText()).isEqualTo(CTA);
    }

    @Test
    @DisplayName("잔여 수량이 없으면 게재하지 않는다")
    void sync_doesNotServeExhaustedCampaign() {
        when(adpickCampaignClient.fetchCampaigns()).thenReturn(List.of(campaign("16b04", "Android", 0)));

        syncService.sync();

        ArgumentCaptor<AdCreative> captor = ArgumentCaptor.forClass(AdCreative.class);
        verify(adCreativeRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(AdStatus.DRAFT);
    }

    @Test
    @DisplayName("이미지나 추적 링크가 없는 캠페인은 건너뛴다")
    void sync_skipsUnrenderableCampaign() {
        AdpickCampaignResponse broken = new AdpickCampaignResponse(
                "16b04", "제목", null, null, Map.of(), null, "Android", 10);
        when(adpickCampaignClient.fetchCampaigns()).thenReturn(List.of(broken));

        assertThat(syncService.sync()).isZero();
        verify(adCreativeRepository, never()).save(any());
    }

    @Test
    @DisplayName("기존 소재는 내용을 갱신하고 새로 만들지 않는다")
    void sync_updatesExistingCreative() {
        AdCreative existing = managed("16b04", AdStatus.ACTIVE);
        when(adCreativeRepository.findAllBySource(AdSource.ADPICK_API)).thenReturn(List.of(existing));
        when(adpickCampaignClient.fetchCampaigns()).thenReturn(List.of(campaign("16b04", "Android", 50)));

        syncService.sync();

        verify(adCreativeRepository, never()).save(any());
        assertThat(existing.getTitle()).isEqualTo("리워디 월렛 - 금테크 지갑");
        assertThat(existing.getLandingUrl()).isEqualTo("https://deg.kr/39e859f");
    }

    @Test
    @DisplayName("어드민이 꺼둔 소재는 동기화가 다시 켜지 않는다")
    void sync_keepsAdminPause() {
        AdCreative paused = managed("16b04", AdStatus.PAUSED);
        when(adCreativeRepository.findAllBySource(AdSource.ADPICK_API)).thenReturn(List.of(paused));
        when(adpickCampaignClient.fetchCampaigns()).thenReturn(List.of(campaign("16b04", "Android", 120)));

        syncService.sync();

        assertThat(paused.getStatus()).isEqualTo(AdStatus.PAUSED);
    }

    @Test
    @DisplayName("피드에서 사라진 캠페인은 지우지 않고 내린다")
    void sync_retiresMissingCampaign() {
        AdCreative gone = managed("old01", AdStatus.ACTIVE);
        when(adCreativeRepository.findAllBySource(AdSource.ADPICK_API)).thenReturn(List.of(gone));
        when(adpickCampaignClient.fetchCampaigns()).thenReturn(List.of(campaign("16b04", "Android", 10)));

        syncService.sync();

        assertThat(gone.getStatus()).isEqualTo(AdStatus.DRAFT);
        verify(adCreativeRepository, never()).delete(any());
    }
}

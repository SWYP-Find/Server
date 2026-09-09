package com.swyp.picke.domain.admin.service;

import com.swyp.picke.domain.ad.entity.AdCreative;
import com.swyp.picke.domain.ad.enums.AdSource;
import com.swyp.picke.domain.ad.enums.AdNetwork;
import com.swyp.picke.domain.ad.enums.AdSlotCode;
import com.swyp.picke.domain.ad.enums.AdStatus;
import com.swyp.picke.domain.ad.enums.AdTargetOs;
import com.swyp.picke.domain.ad.service.AdCreativeCodeGenerator;
import com.swyp.picke.domain.ad.repository.AdClickLogRepository;
import com.swyp.picke.domain.ad.repository.AdCreativeRepository;
import com.swyp.picke.domain.ad.repository.AdImpressionDailyRepository;
import com.swyp.picke.domain.admin.dto.ad.request.AdCreativeRequest;
import com.swyp.picke.global.common.exception.CustomException;
import com.swyp.picke.global.common.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import java.util.Optional;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AdminAdServiceTest {

    private static final String OUR_PARTNERS_ID = "AF6830373";

    @Mock
    private AdCreativeRepository adCreativeRepository;
    @Mock
    private AdClickLogRepository adClickLogRepository;
    @Mock
    private AdImpressionDailyRepository adImpressionDailyRepository;
    @Mock
    private AdCreativeCodeGenerator adCreativeCodeGenerator;

    @InjectMocks
    private AdminAdService adminAdService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(adminAdService, "coupangPartnersId", OUR_PARTNERS_ID);
        when(adCreativeCodeGenerator.generate()).thenReturn("abc23456");
        when(adCreativeRepository.save(any(AdCreative.class))).thenAnswer(call -> call.getArgument(0));
    }

    private AdCreativeRequest request(AdNetwork network, String landingUrl) {
        return new AdCreativeRequest(
                network,
                AdSlotCode.HOME_FEED,
                "무선 이어폰",
                null,
                "https://img.example.com/1.jpg",
                "구매하러 가기",
                landingUrl,
                AdStatus.ACTIVE,
                AdTargetOs.ALL,
                1,
                null,
                null
        );
    }

    @Test
    @DisplayName("형식이 깨진 제휴 링크는 등록 시점에 막는다. 클릭 시점 500을 앞당겨 잡는다")
    void create_rejectsMalformedLandingUrl() {
        // 인코딩되지 않은 공백이 들어간 URL. 클릭 시점 AffiliateLinks.merge 가 build(true) 로 파싱하다 터진다.
        AdCreativeRequest request = request(AdNetwork.ADPICK, "https://deg.kr/a b c");

        assertThatThrownBy(() -> adminAdService.create(request))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.AD_INVALID_LANDING_URL);

        verify(adCreativeRepository, never()).save(any(AdCreative.class));
    }

    @Test
    @DisplayName("스킴이나 호스트가 없는 링크도 막는다")
    void create_rejectsLandingUrlWithoutSchemeOrHost() {
        AdCreativeRequest request = request(AdNetwork.ADPICK, "/c/relative-only");

        assertThatThrownBy(() -> adminAdService.create(request))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.AD_INVALID_LANDING_URL);
    }

    @Test
    @DisplayName("애드픽 단축 링크처럼 파라미터 없는 정상 URL은 통과한다")
    void create_acceptsShortAffiliateLink() {
        AdCreativeRequest request = request(AdNetwork.ADPICK, "https://deg.kr/884a6d6");

        assertThatCode(() -> adminAdService.create(request)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("남의 파트너스 아이디가 박힌 쿠팡 링크는 등록을 막는다")
    void create_rejectsForeignPartnerLink() {
        AdCreativeRequest request = request(AdNetwork.COUPANG,
                "https://link.coupang.com/re/AFF?lptag=AF9999999&pageKey=1");

        assertThatThrownBy(() -> adminAdService.create(request))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.AD_COUPANG_PARTNER_MISMATCH);

        verify(adCreativeRepository, never()).save(any());
    }

    @Test
    @DisplayName("우리 파트너스 아이디면 통과한다")
    void create_allowsOwnPartnerLink() {
        AdCreativeRequest request = request(AdNetwork.COUPANG,
                "https://link.coupang.com/re/AFF?lptag=" + OUR_PARTNERS_ID + "&pageKey=1");

        assertThatCode(() -> adminAdService.create(request)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("lptag가 드러나지 않는 단축 링크는 막지 않는다")
    void create_allowsShortLinkWithoutLptag() {
        AdCreativeRequest request = request(AdNetwork.COUPANG, "https://link.coupang.com/a/abcdef");

        assertThatCode(() -> adminAdService.create(request)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("애드픽 소재는 쿠팡 아이디 검증 대상이 아니다")
    void create_skipsValidationForOtherNetworks() {
        AdCreativeRequest request = request(AdNetwork.ADPICK, "https://adpick.co.kr/?lptag=AF9999999");

        assertThatCode(() -> adminAdService.create(request)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("동기화 소재는 수정·삭제를 막는다")
    void update_rejectsManagedCreative() {
        AdCreative managed = AdCreative.builder()
                .code("syn00001")
                .network(AdNetwork.ADPICK)
                .slot(AdSlotCode.BATTLE_RESULT_BOTTOM)
                .title("리워디 월렛")
                .imageUrl("https://img.example.com/1.jpg")
                .ctaText("설치하고 받기")
                .landingUrl("https://deg.kr/39e859f")
                .status(AdStatus.ACTIVE)
                .weight(1)
                .source(AdSource.ADPICK_API)
                .externalId("16b04")
                .targetOs(AdTargetOs.ANDROID)
                .build();
        when(adCreativeRepository.findById(1L)).thenReturn(Optional.of(managed));

        AdCreativeRequest request = request(AdNetwork.ADPICK, "https://deg.kr/39e859f");

        assertThatThrownBy(() -> adminAdService.update(1L, request))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.AD_CREATIVE_MANAGED);
        assertThatThrownBy(() -> adminAdService.delete(1L))
                .isInstanceOf(CustomException.class);
    }

    @Test
    @DisplayName("동기화 소재도 게재 상태는 바꿀 수 있다")
    void changeStatus_allowedForManagedCreative() {
        AdCreative managed = AdCreative.builder()
                .code("syn00001")
                .network(AdNetwork.ADPICK)
                .slot(AdSlotCode.BATTLE_RESULT_BOTTOM)
                .title("리워디 월렛")
                .imageUrl("https://img.example.com/1.jpg")
                .ctaText("설치하고 받기")
                .landingUrl("https://deg.kr/39e859f")
                .status(AdStatus.ACTIVE)
                .weight(1)
                .source(AdSource.ADPICK_API)
                .externalId("16b04")
                .targetOs(AdTargetOs.ANDROID)
                .build();
        when(adCreativeRepository.findById(1L)).thenReturn(Optional.of(managed));

        assertThat(adminAdService.changeStatus(1L, AdStatus.PAUSED).status()).isEqualTo(AdStatus.PAUSED);
    }
}

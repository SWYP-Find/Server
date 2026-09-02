package com.swyp.picke.domain.admin.service;

import com.swyp.picke.domain.ad.entity.AdCreative;
import com.swyp.picke.domain.ad.enums.AdNetwork;
import com.swyp.picke.domain.ad.enums.AdSlotCode;
import com.swyp.picke.domain.ad.enums.AdStatus;
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
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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

    @InjectMocks
    private AdminAdService adminAdService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(adminAdService, "coupangPartnersId", OUR_PARTNERS_ID);
        when(adCreativeRepository.existsByCode(anyString())).thenReturn(false);
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
                1,
                null,
                null
        );
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
    @DisplayName("소재 코드는 헷갈리는 글자 없이 만들어진다")
    void create_generatesReadableCode() {
        AdCreativeRequest request = request(AdNetwork.COUPANG, "https://link.coupang.com/a/abcdef");

        String code = adminAdService.create(request).code();

        assertThat(code).hasSize(8);
        assertThat(code).doesNotContain("l", "o", "0", "1");
    }
}

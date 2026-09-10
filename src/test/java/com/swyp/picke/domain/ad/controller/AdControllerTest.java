package com.swyp.picke.domain.ad.controller;

import com.swyp.picke.domain.ad.dto.response.AdResponse;
import com.swyp.picke.domain.ad.enums.AdNetwork;
import com.swyp.picke.domain.ad.enums.AdSlotCode;
import com.swyp.picke.domain.ad.enums.AdTargetOs;
import com.swyp.picke.domain.ad.service.AdQueryService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AdControllerTest {
    @Test
    void previewUsesExactUserResponseWithoutRecordingImpressions() throws Exception {
        AdQueryService service = mock(AdQueryService.class);
        AdResponse ad = new AdResponse("sample01", AdNetwork.ADPICK, "사용자 광고", "상품 설명",
                "https://example.com/image.jpg", "구매하러 가기", "https://ad.picke.store/c/sample01", "광고");
        when(service.findServableAds(AdSlotCode.HOME_FEED, AdTargetOs.IOS, 5)).thenReturn(List.of(ad));
        MockMvcBuilders.standaloneSetup(new AdController(service)).build()
                .perform(get("/api/v1/ads").accept(org.springframework.http.MediaType.APPLICATION_JSON).param("slot", "HOME_FEED").param("os", "IOS").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].code").value("sample01"))
                .andExpect(jsonPath("$.data[0].title").value("사용자 광고"))
                .andExpect(jsonPath("$.data[0].label").value("광고"));
        verify(service).findServableAds(AdSlotCode.HOME_FEED, AdTargetOs.IOS, 5);
        verifyNoMoreInteractions(service);
    }
}

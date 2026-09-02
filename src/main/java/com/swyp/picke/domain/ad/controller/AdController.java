package com.swyp.picke.domain.ad.controller;

import com.swyp.picke.domain.ad.dto.request.AdImpressionRequest;
import com.swyp.picke.domain.ad.dto.response.AdResponse;
import com.swyp.picke.domain.ad.enums.AdSlotCode;
import com.swyp.picke.domain.ad.enums.AdTargetOs;
import com.swyp.picke.domain.ad.service.AdQueryService;
import com.swyp.picke.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "제휴 광고 API", description = "앱 지면에 노출할 제휴 광고 조회 및 노출 집계")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/ads")
public class AdController {

    private final AdQueryService adQueryService;

    @Operation(summary = "지면별 광고 조회",
            description = "게재 가능한 소재가 없으면 빈 배열을 준다. 앱은 이때 지면 자체를 숨긴다.")
    @GetMapping
    public ApiResponse<List<AdResponse>> getAds(
            @Parameter(description = "노출 지면", example = "HOME_FEED")
            @RequestParam AdSlotCode slot,
            @Parameter(description = "요청 OS. 앱 설치형 캠페인이 OS별로 갈리므로 실제 OS를 보내야 한다.",
                    example = "ANDROID")
            @RequestParam(defaultValue = "ALL") AdTargetOs os,
            @Parameter(description = "받아갈 소재 개수", example = "1")
            @RequestParam(defaultValue = "1") int size
    ) {
        return ApiResponse.onSuccess(adQueryService.findServableAds(slot, os, size));
    }

    @Operation(summary = "광고 노출 집계",
            description = "조회가 아니라 실제로 화면에 그려진 시점에 호출한다. 조회를 노출로 세면 CTR이 왜곡된다.")
    @PostMapping("/impressions")
    public ApiResponse<Void> recordImpressions(@Valid @RequestBody AdImpressionRequest request) {
        adQueryService.recordImpressions(request.codes());
        return ApiResponse.onSuccess(null);
    }
}

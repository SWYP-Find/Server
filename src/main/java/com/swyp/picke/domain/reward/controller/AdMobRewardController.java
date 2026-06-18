package com.swyp.picke.domain.reward.controller;

import com.swyp.picke.domain.reward.dto.request.AdMobRewardRequest;
import com.swyp.picke.domain.reward.dto.response.AdMobRewardResponse;
import com.swyp.picke.domain.reward.service.AdMobRewardService;
import com.swyp.picke.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Tag(name = "보상 API", description = "AdMob 광고 보상 관련 API")
@RestController
@RequestMapping("/api/v1/admob")
@RequiredArgsConstructor
public class AdMobRewardController {

    private final AdMobRewardService rewardService;

    /**
     * // 1. AdMob SSV 콜백 수신 엔드포인트
     * 호출 경로: GET /api/v1/admob/reward
     */
    @Operation(summary = "AdMob 보상 콜백 수신")
    @GetMapping("/reward")
    public ApiResponse<AdMobRewardResponse> handleAdMobReward(
            AdMobRewardRequest request) {
        log.info("AdMob SSV 콜백 수신: transaction_id={}", request.transaction_id());

        // 실제 SSV 콜백에는 key_id가 반드시 포함됨 — key_id 없으면 AdMob 콘솔 URL 검증 요청으로 판별
        if (request.transaction_id() == null || request.signature() == null || request.key_id() == null) {
            log.info("AdMob URL 검증 요청 수신 - 200 반환");
            return ApiResponse.onSuccess(AdMobRewardResponse.from("OK"));
        }

        String status = rewardService.processReward(request);
        return ApiResponse.onSuccess(AdMobRewardResponse.from(status));
    }
}

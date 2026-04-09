package com.swyp.picke.domain.reward.controller;

import com.swyp.picke.domain.reward.dto.request.AdMobRewardRequest;
import com.swyp.picke.domain.reward.dto.response.AdMobRewardResponse;
import com.swyp.picke.domain.reward.service.AdMobRewardService;
import com.swyp.picke.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Tag(name = "보상 (Reward)", description = "AdMob 광고 보상 관련 API")
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

        // 서비스에서 "OK" 또는 "Already Processed" 수신
        String status = rewardService.processReward(request);

        // DTO로 감싸서 반환 (명세서의 data { "reward_status": "..." } 구조 완성)
        return ApiResponse.onSuccess(AdMobRewardResponse.from(status));
    }
}
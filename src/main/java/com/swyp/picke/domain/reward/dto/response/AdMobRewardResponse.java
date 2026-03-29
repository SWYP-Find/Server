package com.swyp.picke.domain.reward.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Schema(description = "AdMob 보상 처리 결과 응답")
public class AdMobRewardResponse {

    @Schema(description = "처리 결과 코드 (OK, Already Processed)", example = "OK")
    private final String reward_status;

    public static AdMobRewardResponse from(String status) {
        return AdMobRewardResponse.builder()
                .reward_status(status)
                .build();
    }
}

package com.swyp.picke.domain.reward.dto.request;

import com.swyp.picke.domain.reward.enums.RewardItem;
import com.swyp.picke.global.common.exception.CustomException;
import com.swyp.picke.global.common.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestParam;

@Slf4j
public record AdMobRewardRequest(
        // // 1. 구글 애드몹 공식 파라미터 필드
        String ad_network,
        String ad_unit,        // // ad_unit_id에서 공식 명칭인 ad_unit으로 변경
        String custom_data,
        int reward_amount,
        String reward_item,
        long timestamp,
        String transaction_id,
        String signature,
        String key_id,
        String user_id
) {
    // // 2. 생성자를 통한 쿼리 파라미터 매핑 (@RequestParam)
    public AdMobRewardRequest(
            @RequestParam(value = "ad_network", required = false) String ad_network,
            @RequestParam("ad_unit") String ad_unit,
            @RequestParam(value = "custom_data", required = false) String custom_data,
            @RequestParam("reward_amount") int reward_amount,
            @RequestParam("reward_item") String reward_item,
            @RequestParam("timestamp") long timestamp,
            @RequestParam("transaction_id") String transaction_id,
            @RequestParam("signature") String signature,
            @RequestParam("key_id") String key_id,
            @RequestParam(value = "user_id", required = false) String user_id
    ) {
        this.ad_network = ad_network;
        this.ad_unit = ad_unit;
        this.custom_data = custom_data;
        this.reward_amount = reward_amount;
        this.reward_item = reward_item;
        this.timestamp = timestamp;
        this.transaction_id = transaction_id;
        this.signature = signature;
        this.key_id = key_id;
        this.user_id = user_id;
    }

    // // 3. 유저 식별자 추출 (user_id 우선, 없으면 custom_data 사용)
    @com.fasterxml.jackson.annotation.JsonIgnore
    public Long getUserId() {
        try {
            if (this.user_id != null && !this.user_id.isBlank()) {
                return Long.parseLong(this.user_id);
            }
            if (this.custom_data != null && !this.custom_data.isBlank()) {
                return Long.parseLong(this.custom_data);
            }
            throw new CustomException(ErrorCode.REWARD_INVALID_USER);
        } catch (NumberFormatException e) {
            log.error("유저 ID 파싱 실패: user_id={}, custom_data={}", user_id, custom_data);
            throw new CustomException(ErrorCode.REWARD_INVALID_USER);
        }
    }

    // // 4. 보상 유형 변환 (정해진 Enum이 아니면 기본값 POINT 반환)
    @com.fasterxml.jackson.annotation.JsonIgnore
    public RewardItem getRewardType() {
        if (this.reward_item == null || this.reward_item.isBlank()) {
            return RewardItem.POINT;
        }
        try {
            return RewardItem.valueOf(this.reward_item.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("정의되지 않은 reward_item: {}. 기본값 POINT로 처리합니다.", this.reward_item);
            return RewardItem.POINT;
        }
    }
}
package com.swyp.picke.domain.reward.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.swyp.picke.domain.reward.enums.RewardItem;
import com.swyp.picke.global.common.exception.CustomException;
import com.swyp.picke.global.common.exception.ErrorCode;
import org.springframework.web.bind.annotation.RequestParam;

public record AdMobRewardRequest(
        String ad_unit_id,
        String custom_data,
        int reward_amount,
        String reward_item,
        long timestamp,
        String transaction_id,
        String signature,
        String key_id
) {
    // 직접 지정
    public AdMobRewardRequest(
            @RequestParam("ad_unit_id") String ad_unit_id,
            @RequestParam("custom_data") String custom_data,
            @RequestParam("reward_amount") int reward_amount,
            @RequestParam("reward_item") String reward_item,
            @RequestParam("timestamp") long timestamp,
            @RequestParam("transaction_id") String transaction_id,
            @RequestParam("signature") String signature,
            @RequestParam("key_id") String key_id
    ) {
        this.ad_unit_id = ad_unit_id;
        this.custom_data = custom_data;
        this.reward_amount = reward_amount;
        this.reward_item = reward_item;
        this.timestamp = timestamp;
        this.transaction_id = transaction_id;
        this.signature = signature;
        this.key_id = key_id;
    }
    // 구글이 보낸 유저 데이터를 우리 데이터베이스에서 찾기 위해 메소드 추가
    public Long getUserId() {
        try {
            return Long.parseLong(this.custom_data);
        } catch (NumberFormatException e) {
            throw new CustomException(ErrorCode.REWARD_INVALID_USER);
        }
    }

    // 실제 우리가 제공하는 보상 유형이랑 동일한지 확인 enum에서!
    public RewardItem getRewardType() {
        try {
            return RewardItem.valueOf(this.reward_item.toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new CustomException(ErrorCode.REWARD_INVALID_TYPE);
        }
    }
}

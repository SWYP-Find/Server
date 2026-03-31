package com.swyp.picke.domain.reward.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.swyp.picke.domain.reward.enums.RewardItem;
import com.swyp.picke.global.common.exception.CustomException;
import com.swyp.picke.global.common.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestParam;

@Slf4j
public record AdMobRewardRequest(
        String ad_network,
        String ad_unit,
        String custom_data,
        int reward_amount,
        String reward_item,
        long timestamp,
        String transaction_id,
        String signature,
        String key_id,
        String user_id
) {
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

    // // 1. 유저 태그(문자열)를 꺼내는 메서드
    @JsonIgnore
    public String getUserTag() {
        if (this.custom_data != null && !this.custom_data.isBlank()) {
            return this.custom_data;
        }
        return this.user_id;
    }

    @JsonIgnore
    public RewardItem getRewardType() {
        if (this.reward_item == null || this.reward_item.isBlank()) {
            return RewardItem.POINT;
        }
        try {
            if (this.reward_item == null) return RewardItem.POINT;
            return RewardItem.POINT; // 실서비스 안전을 위해 POINT로 고정하거나 로직 유지
        } catch (Exception e) {
            return RewardItem.POINT;
        }
    }
}
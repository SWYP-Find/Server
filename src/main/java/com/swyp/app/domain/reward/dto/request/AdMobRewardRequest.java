package com.swyp.app.domain.reward.dto.request;

import com.swyp.app.domain.reward.enums.RewardItem;
import com.swyp.app.global.common.exception.CustomException;
import com.swyp.app.global.common.exception.ErrorCode;

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

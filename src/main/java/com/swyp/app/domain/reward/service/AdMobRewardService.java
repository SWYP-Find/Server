package com.swyp.app.domain.reward.service;

import com.swyp.app.domain.reward.dto.request.AdMobRewardRequest;

// 서비스를 인터페이스로 분리하면 서비스를 변경할 때, Impl 파일만 수정하면 됨!
// 테스트 코드 짜기 용이!
public interface AdMobRewardService {

    String processReward(AdMobRewardRequest request);

}

package com.swyp.picke.domain.ad.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 광고 노출 지면.
 *
 * <p>테이블이 아니라 enum인 이유는, 앱이 그릴 줄 모르는 지면을 어드민에서 만들어봐야
 * 아무 일도 일어나지 않기 때문이다. 지면 추가는 어차피 앱 배포와 묶인다.
 */
@Getter
@RequiredArgsConstructor
public enum AdSlotCode {

    HOME_FEED("홈 피드 인라인"),
    BATTLE_RESULT_BOTTOM("배틀 결과 하단");

    private final String description;
}

package com.swyp.picke.domain.ad.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 광고 노출 지면.
 *
 * <p>테이블이 아니라 enum인 이유는, 앱이 그릴 줄 모르는 지면을 어드민에서 만들어봐야
 * 아무 일도 일어나지 않기 때문이다. 지면 추가는 어차피 앱 배포와 묶인다.
 *
 * <p>목록은 iOS Presentation 모듈(Home/Battle/Chat/Profile)의 실제 화면을 기준으로 잡았다.
 * 앱팀 확정 전이므로 실제로 붙이는 지면만 소재를 등록하면 된다.
 * 소재가 없는 지면은 빈 배열을 반환하고 앱은 지면 자체를 숨긴다.
 *
 * <p>{@code cpiFriendly}는 앱 설치형(CPI) 광고를 놓아도 되는 지면인지를 뜻한다.
 * CPI는 단가가 높지만 클릭하면 사용자가 스토어로 나가 다른 앱을 설치한다.
 * 세션이 자연스럽게 끝나는 지점이 아니면 이탈·리텐션에 그대로 타격이 온다.
 */
@Getter
@RequiredArgsConstructor
public enum AdSlotCode {

    HOME_FEED("홈 피드 인라인", false),
    BATTLE_RESULT_BOTTOM("배틀 결과 하단", true),
    CHAT_ROOM_INLINE("관점 목록 인라인", false),
    ATTENDANCE_COMPLETE("출석 완료 후", true),
    PROFILE_BOTTOM("프로필 하단", true);

    private final String description;

    /** 앱 설치형(CPI) 광고를 놓아도 되는 지면인지. */
    private final boolean cpiFriendly;
}

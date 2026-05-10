package com.swyp.picke.domain.user.enums;

import lombok.Getter;

@Getter
public enum CreditType {
    TODAY_CREDIT(5),        // 출석체크: 하루 1회
    DEFAULT_CREDIT(30),     // 기본 크레딧: 회원가입 시 기본 지급
    BATTLE_VOTE(5),         // 배틀 참여 보상: 사후 투표 완료 시 즉시 지급
    BATTLE_ENTRY(-5),       // 지난 배틀 이용 비용: 사전 투표 최초 진입 시 차감
    MAJORITY_WIN(10),       // 다수결 보상: 월요일 배치, 2주 전 배틀 TOP≥10 대상
    BEST_COMMENT(15),       // 베댓 보상: 월요일 배치, 2주 전 배틀 좋아요 1위
    WEEKLY_CHARGE(40),      // 주간 자동 충전: 매주 월요일 00:00 활성 사용자 전체
    FREE_CHARGE(20),        // 광고/자유 충전: 가변 금액
    TOPIC_SUGGEST(100),     // 주제 제안
    TOPIC_ADOPTED(120);     // 주제 채택

    private final int defaultAmount;

    CreditType(int defaultAmount) {
        this.defaultAmount = defaultAmount;
    }
}

package com.swyp.picke.domain.user.enums;

public enum UserBattleStep {
    NONE,           // 배틀에 아예 관여하지 않음 (초기 상태)
    PRE_VOTE,       // 상세 진입, 사전 투표 전
    TTS_LISTENING,  // 사전 투표 완료, 오디오 듣는 중
    POST_VOTE,      // 오디오 청취 완료, 사후 투표 대기
    COMPLETED       // 사후 투표까지 완료
}
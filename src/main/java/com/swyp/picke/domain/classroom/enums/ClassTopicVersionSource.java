package com.swyp.picke.domain.classroom.enums;

public enum ClassTopicVersionSource {
    CANDIDATE,  // AI 후보를 그대로 선택 (AI 추가 호출 없음)
    EDITED      // 운영자가 직접 수정하고 AI 의미 검증을 통과
}

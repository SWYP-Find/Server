package com.swyp.picke.domain.classroom.enums;

/** 불완전 JSON·거절·한도 종료는 SUCCEEDED 로 저장하지 않는다. */
public enum AiJobStatus {
    QUEUED, RUNNING, SUCCEEDED, FAILED, REJECTED
}

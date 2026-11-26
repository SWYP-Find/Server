package com.swyp.picke.domain.admin.dto.classroom.response;

import com.swyp.picke.domain.classroom.enums.AiJobStatus;
import com.swyp.picke.domain.classroom.enums.AiJobType;

/** AI 작업 실패를 작업 종류·오류 코드별로 센 줄. 반려된 반박 입력(정상 동작)은 넣지 않는다. */
public record AdminAiErrorRow(AiJobType type, AiJobStatus status, String errorCode, long count) {}

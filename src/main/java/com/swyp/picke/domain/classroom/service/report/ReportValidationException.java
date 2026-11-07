package com.swyp.picke.domain.classroom.service.report;

import lombok.Getter;

/**
 * AI 응답이 규칙(근거 검증·길이·JSON 형식)을 어겨 게시할 수 없을 때. 제공자 장애와 구분해 ai_jobs.error_code 에 기록한다.
 */
@Getter
public class ReportValidationException extends RuntimeException {

    private final String code;

    public ReportValidationException(String code) {
        super(code);
        this.code = code;
    }
}

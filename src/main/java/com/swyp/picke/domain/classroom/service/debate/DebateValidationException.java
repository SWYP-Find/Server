package com.swyp.picke.domain.classroom.service.debate;

import lombok.Getter;

/** AI 주장 응답이 출력 계약을 어겨 게시할 수 없을 때. code 는 ai_jobs.error_code, problem 은 보정 호출에 쓴다 */
@Getter
public class DebateValidationException extends RuntimeException {

    private final String code;
    private final String problem;

    public DebateValidationException(String code, String problem) {
        super(code + " " + problem);
        this.code = code;
        this.problem = problem;
    }
}

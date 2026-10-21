package com.swyp.picke.domain.classroom.service.topic;

import java.util.List;
import lombok.Getter;

/**
 * AI 응답이 주제 규칙(개수·길이·철학자 ID·조건 일치·JSON 형식)을 어겨 게시할 수 없을 때.
 * problems 는 보정 호출에 그대로 넘겨 고치게 한다. 제공자 장애와 구분해 ai_jobs.error_code 에 code 를 기록한다.
 */
@Getter
public class TopicValidationException extends RuntimeException {

    private final String code;
    private final List<String> problems;

    public TopicValidationException(String code, List<String> problems) {
        super(code + " " + problems);
        this.code = code;
        this.problems = problems;
    }

    public TopicValidationException(String code) {
        this(code, List.of());
    }
}

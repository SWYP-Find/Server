package com.swyp.picke.domain.classroom.service.debate;

import com.swyp.picke.domain.classroom.enums.ClassDebateFocus;
import com.swyp.picke.domain.classroom.service.report.ReportContent;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * AI 주장 응답 검사 (PRD §11 출력 계약). 길이는 잘라 쓰지 않고 위반으로 본다.
 * 라운드·종료 여부를 정하는 필드가 있으면 받지 않는다 (부록 A "done·round 값이 응답에 있으면 허용하지 않는다").
 */
@Component
public class ClassDebateContentValidator {

    /** 기획 Q&A 확정 길이 (ERD ai_jobs 노트) */
    public static final int ARGUMENT_MIN_LENGTH = 50;
    public static final int ARGUMENT_MAX_LENGTH = 500;

    private static final Set<String> FORBIDDEN_FIELDS = Set.of("round", "round_index", "done", "finished", "next_round", "winner", "score");

    public record Argument(String text, ClassDebateFocus focus) {
    }

    /** 위반이면 {@link DebateValidationException} (보정 호출에 problem 을 넘긴다) */
    public Argument parse(Map<String, Object> json) {
        for (String field : FORBIDDEN_FIELDS) {
            if (json.containsKey(field)) {
                throw new DebateValidationException("FORBIDDEN_FIELD", field + " 필드는 넣지 않습니다. argument_text 와 focus 만 출력하세요");
            }
        }
        String text = ReportContent.str(json, "argument_text");
        text = text == null ? null : text.strip();
        if (text == null || text.isEmpty()) {
            throw new DebateValidationException("EMPTY_ARGUMENT", "argument_text 가 비어 있습니다");
        }
        int length = text.codePointCount(0, text.length());
        if (length < ARGUMENT_MIN_LENGTH || length > ARGUMENT_MAX_LENGTH) {
            throw new DebateValidationException("ARGUMENT_LENGTH",
                    "argument_text 는 " + ARGUMENT_MIN_LENGTH + "자 이상 " + ARGUMENT_MAX_LENGTH + "자 이하여야 합니다 (현재 " + length + "자)");
        }
        ClassDebateFocus focus = parseFocus(ReportContent.str(json, "focus"));
        if (focus == null) {
            throw new DebateValidationException("INVALID_FOCUS",
                    "focus 는 EVIDENCE, ASSUMPTION, COUNTEREXAMPLE, CONDITION, VALUE_CONFLICT 중 하나여야 합니다");
        }
        return new Argument(text, focus);
    }

    private static ClassDebateFocus parseFocus(String value) {
        if (value == null) {
            return null;
        }
        String v = value.strip();
        for (ClassDebateFocus focus : ClassDebateFocus.values()) {
            if (focus.name().equals(v) || focus.getLabel().equals(v)) {
                return focus;
            }
        }
        return null;
    }
}

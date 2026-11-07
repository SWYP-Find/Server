package com.swyp.picke.domain.classroom.enums;

import lombok.Getter;

/** AI 주장의 초점 (PRD §11 출력 계약 focus) */
@Getter
public enum ClassDebateFocus {
    EVIDENCE("근거"),
    ASSUMPTION("가정"),
    COUNTEREXAMPLE("반례"),
    CONDITION("조건"),
    VALUE_CONFLICT("가치충돌");

    private final String label;

    ClassDebateFocus(String label) {
        this.label = label;
    }
}

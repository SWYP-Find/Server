package com.swyp.picke.domain.classroom.service.topic;

/**
 * 주제 수정안의 필드별 문제 (부록 A "주제 수정 검증 결과"의 issues 항목).
 * field 는 question, description, options[i].label, options[i].rationale, options[i].philosopher, options 중 하나.
 */
public record TopicIssue(String field, String code, String message) {
}

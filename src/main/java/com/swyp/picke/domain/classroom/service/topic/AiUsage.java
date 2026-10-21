package com.swyp.picke.domain.classroom.service.topic;

import com.swyp.picke.domain.classroom.service.report.ClassReportAiClient;

/** 업무 작업 1건(제안 배치 또는 수정 검증)에서 쓴 제공자 호출 수와 토큰 합계 (PRD §9, §21) */
public final class AiUsage {

    private int calls;
    private Integer inputTokens;
    private Integer outputTokens;

    public void addCall() {
        calls++;
    }

    public void add(ClassReportAiClient.AiChatResult result) {
        calls++;
        inputTokens = sum(inputTokens, result.inputTokens());
        outputTokens = sum(outputTokens, result.outputTokens());
    }

    public int calls() {
        return calls;
    }

    public Integer inputTokens() {
        return inputTokens;
    }

    public Integer outputTokens() {
        return outputTokens;
    }

    private static Integer sum(Integer total, Integer value) {
        if (value == null) {
            return total;
        }
        return total == null ? value : total + value;
    }
}

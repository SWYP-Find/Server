package com.swyp.picke.domain.classroom.service.report;

import java.util.regex.Pattern;

/**
 * 리포트 AI 입력에서 비속어를 *** 로 가린다 (엣지 케이스 PRD RP-07 "유효한 논거만 분석", 2.4 "비속어를 따라 하지 않음").
 * 비속어가 섞이면 모델이 글 전체를 분석 불가로 버리는 경우가 많아, 논거만 남기고 넘긴다.
 * 오탐을 줄이려고 뜻이 분명한 표현만 넣고 일반 단어("시발점", "새끼손가락")는 제외한다. 원문(DB)은 바꾸지 않는다.
 */
public final class ReportProfanityMasker {

    public static final String MASK = "***";

    private static final Pattern PROFANITY = Pattern.compile(
            "씨\\s*발|씨\\s*바|씨\\s*빨|쓰\\s*발|시\\s*발(?!점)|ㅅ\\s*ㅂ|ㅆ\\s*ㅂ|ㅄ|ㅂ\\s*ㅅ|병\\s*신|븅\\s*신"
                    + "|좆|존\\s*나|ㅈ\\s*ㄴ|지\\s*랄|ㅈ\\s*ㄹ|염\\s*병|개\\s*소리|개\\s*같|개\\s*새끼|새\\s*끼(?!\\s*손가락)"
                    + "|미친\\s*놈|미친\\s*년|쌍\\s*년|쌍\\s*놈|닥\\s*쳐|꺼\\s*져|엿\\s*먹|니\\s*미|느\\s*금");

    private ReportProfanityMasker() {
    }

    public static String mask(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        return PROFANITY.matcher(text).replaceAll(MASK);
    }
}

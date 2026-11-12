package com.swyp.picke.domain.classroom.service.debate;

import java.util.HashSet;
import java.util.Set;

/**
 * 반복 문자·자음처럼 내용을 알 수 없는 반박을 AI 호출 없이 거른다 (엣지 케이스 PRD EC-07 "입력 단계에서 반려, 라운드 미소모").
 * 50자 제한을 의미 없는 글자로 채운 경우만 잡도록 기준을 보수적으로 둔다. 애매한 입력은 입력 분류 AI 가 다시 본다.
 */
final class MeaninglessInputDetector {

    /** 공백·문장부호를 뺀 글자가 이보다 적으면 내용이 없다고 본다 */
    static final int MIN_LETTERS = 10;
    /** 서로 다른 글자가 이보다 적으면 반복으로 본다 (정상 문장 50자는 보통 20자 이상) */
    static final int MIN_DISTINCT_LETTERS = 8;
    /** 자음·모음 낱자 비율이 이보다 크면 의미 없는 입력으로 본다 (ㅋㅋ, ㄱㄴㄷ) */
    static final double MAX_JAMO_RATIO = 0.5;

    private MeaninglessInputDetector() {
    }

    static boolean isMeaningless(String text) {
        if (text == null) {
            return true;
        }
        int letters = 0;
        int jamo = 0;
        Set<Integer> distinct = new HashSet<>();
        for (int i = 0; i < text.length(); ) {
            int cp = text.codePointAt(i);
            i += Character.charCount(cp);
            if (!Character.isLetterOrDigit(cp)) {
                continue;
            }
            letters++;
            distinct.add(Character.toLowerCase(cp));
            if (cp >= 0x3131 && cp <= 0x318E) {
                jamo++;
            }
        }
        if (letters < MIN_LETTERS || distinct.size() < MIN_DISTINCT_LETTERS) {
            return true;
        }
        return (double) jamo / letters > MAX_JAMO_RATIO;
    }
}

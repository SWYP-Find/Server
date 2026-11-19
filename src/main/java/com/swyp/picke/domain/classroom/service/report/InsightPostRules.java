package com.swyp.picke.domain.classroom.service.report;

import java.util.regex.Pattern;

/**
 * 클래스 인사이트에 쓸 공개 글 기준 (#657).
 * <ul>
 *   <li>hasReason: 글자·숫자가 {@value #MIN_MEANINGFUL_CHARS}자 이상이고 서로 다른 글자가 {@value #MIN_DISTINCT_CHARS}개 이상인 글만
 *       AI 입력과 작성자 수에 넣는다. "몰라요 그냥요", "ㅋㅋㅋㅋ", "ㅁㄴㅇㄹ", "asdfasdf", "11111111", "아아아아아아아아" 처럼
 *       이유를 읽을 수 없는 글은 요약할 내용이 없다. 자판을 아무렇게나 누른 긴 글("qwerasdf")은 프롬프트에서 제외시킨다</li>
 *   <li>isSelectable: 근거·대표 의견으로 운영자 화면에 보여줄 수 있는 글. 이유가 있고 비속어가 없어야 한다</li>
 * </ul>
 * 실명 지목·조롱처럼 서버가 판별하기 어려운 글은 프롬프트로 막는다.
 */
public final class InsightPostRules {

    static final int MIN_MEANINGFUL_CHARS = 8;
    static final int MIN_DISTINCT_CHARS = 5;

    /** 한글 완성형·영문·숫자 외(공백, 문장부호, 이모지, ㅋㅎㅠ 같은 자모)는 글자 수에서 뺀다 */
    private static final Pattern NOT_MEANINGFUL = Pattern.compile("[^가-힣A-Za-z0-9]");

    private InsightPostRules() {
    }

    public static boolean hasReason(String content) {
        if (content == null) {
            return false;
        }
        String meaningful = NOT_MEANINGFUL.matcher(content).replaceAll("").toLowerCase(java.util.Locale.ROOT);
        if (meaningful.length() < MIN_MEANINGFUL_CHARS || meaningful.chars().allMatch(Character::isDigit)) {
            return false;
        }
        return meaningful.chars().distinct().count() >= MIN_DISTINCT_CHARS;
    }

    /** content 는 원문이어도, 이미 가린 글이어도 된다 */
    public static boolean isSelectable(String content) {
        return hasReason(content) && !ReportProfanityMasker.mask(content).contains(ReportProfanityMasker.MASK);
    }
}

package com.swyp.picke.domain.classroom.service.report;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * 클래스 인사이트에 쓸 공개 글 기준 (#657, #717).
 * <ul>
 *   <li>hasContent: AI 입력·작성자 수에 넣는 글. 글자·숫자가 {@value #MIN_CONTENT_CHARS}자 이상이면 짧아도 넣는다
 *       ("싸고 좋은게 짱.", "반띵 해야지"). 자모·웃음만 있는 글, 같은 글자·패턴 반복("하하하하", "asdfasdf"), 숫자만 있는 글,
 *       "몰라요 그냥요" 처럼 이유가 없다는 말만 있는 글은 뺀다. 영문만 있는 글은 자판 누르기와 구분이 어려워 hasReason 기준을 쓴다.
 *       주제와 무관한 짧은 글("아무댓글")은 AI 가 프롬프트 규칙으로 뺀다</li>
 *   <li>isSelectable: AI 가 근거·대표 의견으로 고른 글을 운영자 화면에 보여줄 수 있는지. hasContent 이고 비속어가 없어야 한다</li>
 *   <li>hasReason · isAutoSelectable: 서버가 사람·AI 판단 없이 대표·주요 의견을 채울 때 쓰는 더 엄격한 기준
 *       ({@value #MIN_MEANINGFUL_CHARS}자 이상, 서로 다른 글자 {@value #MIN_DISTINCT_CHARS}개 이상)</li>
 * </ul>
 * 실명 지목·조롱처럼 서버가 판별하기 어려운 글은 프롬프트로 막는다.
 */
public final class InsightPostRules {

    static final int MIN_MEANINGFUL_CHARS = 8;
    static final int MIN_DISTINCT_CHARS = 5;
    static final int MIN_CONTENT_CHARS = 4;
    static final int MIN_CONTENT_DISTINCT_CHARS = 3;

    /** 한글 완성형·영문·숫자 외(공백, 문장부호, 이모지, ㅋㅎㅠ 같은 자모)는 글자 수에서 뺀다 */
    private static final Pattern NOT_MEANINGFUL = Pattern.compile("[^가-힣A-Za-z0-9]");
    private static final Pattern HANGUL = Pattern.compile("[가-힣]");
    /** 이유가 없다는 뜻만 있는 말. 빼고 남는 글자로 길이를 잰다 */
    private static final List<String> NO_REASON_WORDS = List.of(
            "모르겠어요", "모르겠음", "모르겠다", "몰라요", "몰라", "그냥요", "그냥", "아무거나", "글쎄요", "글쎄", "인정", "패스", "없음");

    private InsightPostRules() {
    }

    public static boolean hasContent(String content) {
        String meaningful = meaningful(content);
        if (!HANGUL.matcher(meaningful).find()) {
            return hasReason(content);
        }
        for (String word : NO_REASON_WORDS) {
            meaningful = meaningful.replace(word, "");
        }
        if (meaningful.length() < MIN_CONTENT_CHARS || meaningful.chars().allMatch(Character::isDigit) || isRepeated(meaningful)) {
            return false;
        }
        return meaningful.chars().distinct().count() >= MIN_CONTENT_DISTINCT_CHARS;
    }

    public static boolean hasReason(String content) {
        String meaningful = meaningful(content);
        if (meaningful.length() < MIN_MEANINGFUL_CHARS || meaningful.chars().allMatch(Character::isDigit) || isRepeated(meaningful)) {
            return false;
        }
        return meaningful.chars().distinct().count() >= MIN_DISTINCT_CHARS;
    }

    /** AI 가 고른 근거·대표 의견. content 는 원문이어도, 이미 가린 글이어도 된다 */
    public static boolean isSelectable(String content) {
        return hasContent(content) && !ReportProfanityMasker.mask(content).contains(ReportProfanityMasker.MASK);
    }

    /** 서버가 판단 없이 채우는 대표·주요 의견 */
    public static boolean isAutoSelectable(String content) {
        return hasReason(content) && !ReportProfanityMasker.mask(content).contains(ReportProfanityMasker.MASK);
    }

    private static String meaningful(String content) {
        return content == null ? "" : NOT_MEANINGFUL.matcher(content).replaceAll("").toLowerCase(Locale.ROOT);
    }

    /** "하하하하", "asdfasdf", "1234 1234" 처럼 짧은 단위가 처음부터 끝까지 반복되는 글 */
    private static boolean isRepeated(String text) {
        for (int unit = 1; unit <= text.length() / 2; unit++) {
            if (text.length() % unit == 0 && text.substring(0, unit).repeat(text.length() / unit).equals(text)) {
                return true;
            }
        }
        return false;
    }
}

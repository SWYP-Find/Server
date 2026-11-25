package com.swyp.picke.domain.classroom.service.report;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 개인 리포트 품질 검사 (#707). 검증(근거·길이)을 통과한 AI 피드백에서 아래를 찾는다.
 * 문제가 있으면 생성 서비스가 한 번 다시 만들고, 다시 만들어도 남으면 그대로 게시한다 (리포트가 안 나오는 일은 없게).
 * <ul>
 *   <li>GENERIC_TITLE: 잘한 점·살펴볼 점 제목이 관찰 항목 이름 그대로 ("주장과 이유의 연결" 등)</li>
 *   <li>UNSUPPORTED_OPTION_TEXT: 배틀 선택지(제목·입장)·배틀 요약 문장의 구절({@value #OPTION_PHRASE_CHARS}글자)이 리포트에 있는데
 *       참가자 글에는 없음. 참가자가 실제로 비슷하게 쓴 구절은 문제가 아니다</li>
 *   <li>REDUNDANT_STRENGTH: 잘한 점이 판단 기준을 거의 그대로 반복</li>
 *   <li>TONE: 본문이 "~요" 로 끝나지 않거나 "~습니다" 가 섞임</li>
 * </ul>
 */
public final class ReportQualityChecker {

    /**
     * 선택지·요약 문장과 이어서 같아야 하는 글자 수 (공백·기호 제외). 8글자는 '인간의 발전을 위해' 같은 주제어 겹침까지 잡아
     * 11/24 QA 17건 중 15건이 걸렸다. 10글자면 실제로 옮겨 쓴 10건만 걸린다 (#711)
     */
    static final int OPTION_PHRASE_CHARS = 10;
    /** 잘한 점 본문 글자쌍 중 판단 기준과 겹치는 비율. 11/24 QA 17건 중 심한 반복(0.5~0.66)만 걸리도록 */
    static final double REDUNDANT_OVERLAP = 0.5;

    private static final Pattern NOT_TEXT = Pattern.compile("[^가-힣A-Za-z0-9]");
    private static final List<String> CATEGORY_NAMES = List.of(
            "주장과이유의연결", "근거와가정점검", "반대논점에대한응답", "판단조건구체화");

    private ReportQualityChecker() {
    }

    public static List<String> memberFeedbackIssues(Map<String, Object> content, MemberFeedbackInput input) {
        List<String> issues = new ArrayList<>();
        if (content == null || content.get("limited_reason") != null) {
            return issues; // 서버 템플릿은 검사하지 않는다
        }
        Map<String, Object> strength = ReportContent.map(content, "strength");
        Map<String, Object> nextFocus = ReportContent.map(content, "next_focus");
        String strengthTitle = ReportContent.str(strength, "title");
        String strengthBody = ReportContent.str(strength, "body");
        String focusTitle = ReportContent.str(nextFocus, "title");
        String focusBody = ReportContent.str(nextFocus, "body");
        String headline = ReportContent.str(content, "headline");
        String basis = ReportContent.str(content, "judgment_basis");

        if (isCategoryName(strengthTitle) || isCategoryName(focusTitle)) {
            issues.add("GENERIC_TITLE");
        }
        if (hasUnsupportedOptionText(String.join(" ", nz(headline), nz(basis), nz(strengthTitle), nz(strengthBody)), input)) {
            issues.add("UNSUPPORTED_OPTION_TEXT");
        }
        if (overlap(strengthBody, basis) >= REDUNDANT_OVERLAP) {
            issues.add("REDUNDANT_STRENGTH");
        }
        if (badTone(basis) || badTone(strengthBody) || badTone(focusBody)) {
            issues.add("TONE");
        }
        return issues;
    }

    static boolean isCategoryName(String title) {
        return title != null && CATEGORY_NAMES.contains(normalize(title));
    }

    /** 선택지 제목·입장·배틀 요약의 구절이 리포트에 있는데 참가자 글 어디에도 없으면 true */
    static boolean hasUnsupportedOptionText(String reportText, MemberFeedbackInput input) {
        if (input == null || input.options() == null) {
            return false;
        }
        String report = normalize(reportText);
        StringBuilder mine = new StringBuilder();
        for (MemberFeedbackInput.Statement statement : input.statements()) {
            mine.append(normalize(statement.content()));
        }
        String own = mine.toString();
        // 참가자 글이 아닌데 AI 가 참가자 주장처럼 옮겨 쓰기 쉬운 글: 선택지 제목·입장, 배틀 요약 (11/24 QA 클래스 33·34)
        List<String> notOwn = new ArrayList<>();
        for (MemberFeedbackInput.OptionInfo option : input.options()) {
            notOwn.add(normalize(nz(option.title()) + nz(option.stance())));
        }
        notOwn.add(normalize(input.battleSummary()));
        for (String source : notOwn) {
            for (int i = 0; i + OPTION_PHRASE_CHARS <= source.length(); i++) {
                String phrase = source.substring(i, i + OPTION_PHRASE_CHARS);
                if (report.contains(phrase) && !own.contains(phrase)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** a 의 글자쌍 중 b 에도 있는 비율 (0~1) */
    static double overlap(String a, String b) {
        Set<String> left = bigrams(a);
        Set<String> right = bigrams(b);
        if (left.isEmpty() || right.isEmpty()) {
            return 0;
        }
        int shared = 0;
        for (String gram : left) {
            if (right.contains(gram)) {
                shared++;
            }
        }
        return (double) shared / Math.min(left.size(), right.size());
    }

    static boolean badTone(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        if (text.contains("습니다")) {
            return true;
        }
        String trimmed = text.strip().replaceAll("[\\s.!?…~\"'”’)]+$", "");
        return !trimmed.endsWith("요");
    }

    private static Set<String> bigrams(String text) {
        String normalized = normalize(text);
        Set<String> grams = new HashSet<>();
        for (int i = 0; i + 2 <= normalized.length(); i++) {
            grams.add(normalized.substring(i, i + 2));
        }
        return grams;
    }

    private static String normalize(String text) {
        return text == null ? "" : NOT_TEXT.matcher(text).replaceAll("").toLowerCase(java.util.Locale.ROOT);
    }

    private static String nz(String text) {
        return text == null ? "" : text;
    }
}

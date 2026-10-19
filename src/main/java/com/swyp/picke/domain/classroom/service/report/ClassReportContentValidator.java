package com.swyp.picke.domain.classroom.service.report;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * AI 응답을 게시 가능한 content 로 바꾸며 PRD §13·§14 의 근거 검증을 수행한다.
 * <ul>
 *   <li>evidence_refs 의 source_id 는 본인 발언이어야 하고 quote 는 그 원문에 실제로 포함돼야 한다. 아니면 그 ref 를 버린다</li>
 *   <li>judgment_basis 에 유효 근거가 하나도 없으면 게시하지 않는다(EVIDENCE_MISSING). strength / next_focus 는 근거 없으면 null</li>
 *   <li>길이는 규정의 20% 까지만 허용하고 넘으면 거절한다 (자르면 문장이 깨진다)</li>
 * </ul>
 */
@Component
public class ClassReportContentValidator {

    static final int HEADLINE_MAX = 40;
    static final int JUDGMENT_BASIS_MAX = 150;
    static final int SECTION_TITLE_MAX = 20;
    static final int SECTION_BODY_MAX = 120;
    static final int NEXT_QUESTION_MAX = 100;
    static final int INSIGHT_TEXT_MAX = 200;
    static final int INSIGHT_EVIDENCE_MAX = 5;
    static final int INSIGHT_HIGHLIGHT_MAX = 2;
    private static final double LENGTH_TOLERANCE = 1.2;

    static final String FIELD_JUDGMENT_BASIS = "judgment_basis";
    static final String FIELD_STRENGTH = "strength";
    static final String FIELD_NEXT_FOCUS = "next_focus";

    public Map<String, Object> validateMemberFeedback(Map<String, Object> json, MemberFeedbackInput input) {
        String headline = requireText(json, "headline", HEADLINE_MAX);
        String judgmentBasis = requireText(json, "judgment_basis", JUDGMENT_BASIS_MAX);
        String nextQuestion = requireText(json, "next_question", NEXT_QUESTION_MAX);
        Map<String, Object> strength = section(json, FIELD_STRENGTH);
        Map<String, Object> nextFocus = section(json, FIELD_NEXT_FOCUS);

        List<Map<String, Object>> refs = new ArrayList<>();
        Set<String> fieldsWithEvidence = new LinkedHashSet<>();
        for (Map<String, Object> ref : ReportContent.mapList(json, "evidence_refs")) {
            String field = ReportContent.str(ref, "field");
            String sourceId = ReportContent.str(ref, "source_id");
            String quote = ReportContent.str(ref, "quote");
            if (field == null || sourceId == null || quote == null || quote.isBlank()) {
                continue;
            }
            if (!FIELD_JUDGMENT_BASIS.equals(field) && !FIELD_STRENGTH.equals(field) && !FIELD_NEXT_FOCUS.equals(field)) {
                continue;
            }
            MemberFeedbackInput.Statement statement = input.findStatement(sourceId.trim());
            if (statement == null || !contains(statement.content(), quote)) {
                continue; // 존재하지 않는 발언·타인 발언·원문에 없는 인용은 근거로 쓰지 않는다
            }
            Map<String, Object> valid = new LinkedHashMap<>();
            valid.put("field", field);
            valid.put("source_type", statement.sourceType());
            valid.put("source_id", statement.id());
            valid.put("quote", quote.trim());
            refs.add(valid);
            fieldsWithEvidence.add(field);
        }
        if (!fieldsWithEvidence.contains(FIELD_JUDGMENT_BASIS)) {
            throw new ReportValidationException("EVIDENCE_MISSING");
        }
        if (strength != null && !fieldsWithEvidence.contains(FIELD_STRENGTH)) {
            strength = null;
            refs.removeIf(r -> FIELD_STRENGTH.equals(r.get("field")));
        }
        if (nextFocus != null && !fieldsWithEvidence.contains(FIELD_NEXT_FOCUS)) {
            nextFocus = null;
            refs.removeIf(r -> FIELD_NEXT_FOCUS.equals(r.get("field")));
        }

        String analysisStatus = "LIMITED".equalsIgnoreCase(ReportContent.str(json, "analysis_status")) ? "LIMITED" : "SUFFICIENT";
        if (strength == null && nextFocus == null) {
            analysisStatus = "LIMITED";
        }

        Map<String, Object> content = new LinkedHashMap<>();
        content.put("headline", headline);
        content.put("judgment_basis", judgmentBasis);
        content.put("strength", strength);
        content.put("next_focus", nextFocus);
        content.put("next_question", nextQuestion);
        content.put("evidence_refs", refs);
        content.put("analysis_status", analysisStatus);
        content.put("coverage", input.coverage());
        content.put("excluded_count", input.excludedCount());
        content.put("pre_option_id", input.preOption() == null ? null : input.preOption().optionId());
        content.put("post_option_id", input.postOption() == null ? null : input.postOption().optionId());
        return content;
    }

    public Map<String, Object> validateClassInsight(Map<String, Object> json, ClassInsightInput input) {
        String insightText = requireText(json, "insight_text", INSIGHT_TEXT_MAX);

        List<String> evidenceIds = new ArrayList<>();
        for (String id : ReportContent.stringList(json, "evidence_ids")) {
            String trimmed = id.trim();
            if (input.findPost(trimmed) != null && !evidenceIds.contains(trimmed) && evidenceIds.size() < INSIGHT_EVIDENCE_MAX) {
                evidenceIds.add(trimmed);
            }
        }
        if (evidenceIds.isEmpty()) {
            throw new ReportValidationException("EVIDENCE_MISSING");
        }
        List<String> highlightIds = new ArrayList<>();
        for (String id : ReportContent.stringList(json, "highlight_ids")) {
            String trimmed = id.trim();
            if (input.findPost(trimmed) != null && !highlightIds.contains(trimmed) && highlightIds.size() < INSIGHT_HIGHLIGHT_MAX) {
                highlightIds.add(trimmed);
            }
        }

        Map<String, Object> insight = new LinkedHashMap<>();
        insight.put("insight_text", insightText);
        insight.put("evidence_ids", evidenceIds);
        insight.put("highlight_ids", highlightIds);
        insight.put("analysis_status", "SUFFICIENT");
        insight.put("coverage", input.coverage());
        insight.put("excluded_count", input.excludedCount());
        return insight;
    }

    // ===== 템플릿 (AI 호출 없음) =====

    /** 투표만 있을 때의 서버 템플릿 (PRD §13 자료 부족 처리) */
    public static Map<String, Object> limitedMemberFeedback(MemberFeedbackInput.OptionInfo pre, MemberFeedbackInput.OptionInfo post) {
        String judgmentBasis;
        if (pre != null && post != null && !pre.optionId().equals(post.optionId())) {
            judgmentBasis = "선택이 " + pre.title() + "에서 " + post.title() + "로 바뀌었어요.";
        } else if (post != null) {
            judgmentBasis = "처음부터 끝까지 " + post.title() + "를 선택했어요.";
        } else {
            judgmentBasis = "선택 기록을 확인할 수 없어요.";
        }
        Map<String, Object> content = new LinkedHashMap<>();
        content.put("headline", "선택의 이유를 확인할 글이 아직 없어요");
        content.put("judgment_basis", judgmentBasis);
        content.put("strength", null);
        content.put("next_focus", null);
        content.put("next_question", "지금의 선택을 한 가장 중요한 이유는 무엇인가요?");
        content.put("evidence_refs", List.of());
        content.put("analysis_status", "LIMITED");
        content.put("coverage", "full");
        content.put("excluded_count", 0);
        content.put("pre_option_id", pre == null ? null : pre.optionId());
        content.put("post_option_id", post == null ? null : post.optionId());
        return content;
    }

    /** 유효 공개 글 작성자 3명 미만일 때의 템플릿 (PRD §14 개인정보와 소수 의견) */
    public static Map<String, Object> limitedClassInsight() {
        Map<String, Object> insight = new LinkedHashMap<>();
        insight.put("insight_text", "아직 요약할 의견이 충분하지 않아요");
        insight.put("evidence_ids", List.of());
        insight.put("highlight_ids", List.of());
        insight.put("analysis_status", "LIMITED");
        insight.put("coverage", "full");
        insight.put("excluded_count", 0);
        return insight;
    }

    // ===== 헬퍼 =====

    private static String requireText(Map<String, Object> json, String key, int max) {
        String value = ReportContent.str(json, key);
        if (value == null || value.isBlank()) {
            throw new ReportValidationException("MISSING_" + key.toUpperCase());
        }
        value = value.trim();
        if (value.length() > max * LENGTH_TOLERANCE) {
            throw new ReportValidationException("TOO_LONG_" + key.toUpperCase());
        }
        return value;
    }

    private static Map<String, Object> section(Map<String, Object> json, String key) {
        Map<String, Object> raw = ReportContent.map(json, key);
        if (raw == null) {
            return null;
        }
        String title = ReportContent.str(raw, "title");
        String body = ReportContent.str(raw, "body");
        if (title == null || title.isBlank() || body == null || body.isBlank()) {
            return null;
        }
        if (title.trim().length() > SECTION_TITLE_MAX * LENGTH_TOLERANCE || body.trim().length() > SECTION_BODY_MAX * LENGTH_TOLERANCE) {
            throw new ReportValidationException("TOO_LONG_" + key.toUpperCase());
        }
        Map<String, Object> section = new LinkedHashMap<>();
        section.put("title", title.trim());
        section.put("body", body.trim());
        return section;
    }

    /** 공백·줄바꿈 차이는 무시하고 인용이 원문에 들어 있는지 본다 */
    static boolean contains(String content, String quote) {
        if (content == null || quote == null) {
            return false;
        }
        String normalizedQuote = quote.replaceAll("\\s+", "");
        if (normalizedQuote.length() < 2) {
            return false;
        }
        return content.replaceAll("\\s+", "").contains(normalizedQuote);
    }
}

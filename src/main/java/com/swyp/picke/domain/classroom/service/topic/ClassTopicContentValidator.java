package com.swyp.picke.domain.classroom.service.topic;

import static com.swyp.picke.domain.classroom.entity.ClassTopicVersion.DESCRIPTION_MAX_LENGTH;
import static com.swyp.picke.domain.classroom.entity.ClassTopicVersion.OPTION_LABEL_MAX_LENGTH;
import static com.swyp.picke.domain.classroom.entity.ClassTopicVersion.OPTION_RATIONALE_MAX_LENGTH;
import static com.swyp.picke.domain.classroom.entity.ClassTopicVersion.QUESTION_MAX_LENGTH;

import com.swyp.picke.domain.battle.enums.BattleCategory;
import com.swyp.picke.domain.battle.enums.BattleDifficulty;
import com.swyp.picke.domain.classroom.service.report.ReportContent;
import com.swyp.picke.domain.user.enums.PhilosopherType;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * 주제 AI 응답과 운영자 수정안을 서버 규칙으로 검사한다 (PRD §9 "구조화 출력이어도 의미·길이·참조 ID 는 별도 검사", §10).
 * 길이는 잘라 쓰지 않고 위반으로 본다. AI 가 고른 철학자는 허용 목록(enum 코드)에 있을 때만 받는다.
 */
@Component
public class ClassTopicContentValidator {

    public static final int CANDIDATE_COUNT = 3;
    static final int CLARIFICATION_MAX_LENGTH = 200;
    static final String DEFAULT_CLARIFICATION = "이 주제로 토론하려면 다루고 싶은 상황이나 맥락을 조금 더 적어 주세요.";

    private static final Set<String> ISSUE_FIELDS = Set.of(
            "question", "description",
            "options[0].label", "options[0].rationale", "options[0].philosopher",
            "options[1].label", "options[1].rationale", "options[1].philosopher");
    private static final Set<String> ISSUE_CODES = Set.of(
            "OPTIONS_NOT_SAME_AXIS", "OPTION_MISMATCH_QUESTION", "DESCRIPTION_MISMATCH", "RATIONALE_MISMATCH",
            "PHILOSOPHER_MISMATCH", "IMPLIES_ANSWER", "DIGNITY");
    private static final int ISSUE_MESSAGE_MAX_LENGTH = 100;

    /** 제안 응답 해석 결과. 맥락 부족이면 candidates 는 비어 있다 */
    public record SuggestResult(boolean needsClarification, String clarificationMessage, List<TopicContent> candidates) {
    }

    /** 제안 응답을 검사한다. 하나라도 어기면 배치 전체를 받지 않고, 문제 목록을 보정 호출에 넘긴다 */
    public SuggestResult parseSuggestion(Map<String, Object> json, TopicConditions conditions) {
        if (Boolean.TRUE.equals(json.get("needs_clarification"))) {
            String message = trimmed(ReportContent.str(json, "clarification_message"));
            if (message == null || length(message) > CLARIFICATION_MAX_LENGTH) {
                message = DEFAULT_CLARIFICATION;
            }
            return new SuggestResult(true, message, List.of());
        }

        List<Map<String, Object>> raw = ReportContent.mapList(json, "candidates");
        List<String> problems = new ArrayList<>();
        if (raw.size() != CANDIDATE_COUNT) {
            problems.add("candidates 는 정확히 " + CANDIDATE_COUNT + "개여야 합니다 (현재 " + raw.size() + "개)");
        }
        List<TopicContent> candidates = new ArrayList<>();
        Set<String> seenQuestions = new HashSet<>();
        for (int i = 0; i < raw.size(); i++) {
            String at = "candidates[" + i + "]";
            TopicContent candidate = parseCandidate(raw.get(i), conditions, at, problems);
            if (candidate == null) {
                continue;
            }
            if (!seenQuestions.add(normalize(candidate.question()))) {
                problems.add(at + ".question 이 다른 후보와 같습니다. 서로 다른 질문으로 바꾸세요");
            }
            candidates.add(candidate);
        }
        if (!problems.isEmpty()) {
            throw new TopicValidationException("CANDIDATE_RULES", problems);
        }
        return new SuggestResult(false, null, candidates);
    }

    /** 수정안의 형식 검사. 여기서 걸리면 AI 를 호출하지 않고 검증 횟수도 쓰지 않는다 (PRD §10 "서버 규칙으로 걸러지는 형식 오류") */
    public List<TopicIssue> formatIssues(TopicContent edit) {
        List<TopicIssue> issues = new ArrayList<>();
        checkText(issues, "question", edit.question(), QUESTION_MAX_LENGTH, "질문");
        checkText(issues, "description", edit.description(), DESCRIPTION_MAX_LENGTH, "설명");
        List<TopicContent.Option> options = edit.options();
        for (int i = 0; i < options.size(); i++) {
            TopicContent.Option option = options.get(i);
            checkText(issues, "options[" + i + "].label", option.label(), OPTION_LABEL_MAX_LENGTH, "선택지");
            checkText(issues, "options[" + i + "].rationale", option.rationale(), OPTION_RATIONALE_MAX_LENGTH, "관점 설명");
            if (option.philosopher() == null) {
                issues.add(new TopicIssue("options[" + i + "].philosopher", "REQUIRED", "철학자를 골라 주세요."));
            }
        }
        String labelA = trimmed(edit.optionA().label());
        if (labelA != null && labelA.equals(trimmed(edit.optionB().label()))) {
            issues.add(new TopicIssue("options[1].label", "DUPLICATE_OPTION", "두 선택지의 문구가 같아요. 서로 다른 답으로 적어 주세요."));
        }
        if (edit.optionA().philosopher() != null && edit.optionA().philosopher() == edit.optionB().philosopher()) {
            issues.add(new TopicIssue("options[1].philosopher", "SAME_PHILOSOPHER", "두 선택지에는 서로 다른 철학자를 골라 주세요."));
        }
        return issues;
    }

    /**
     * 수정 검증 응답을 해석한다. valid=true 면 빈 목록. 모르는 필드·코드는 버리지 않고 일반 값으로 바꾼다.
     * valid=false 인데 이유가 하나도 없으면 운영자가 고칠 수 없으므로 형식 위반으로 본다.
     */
    public List<TopicIssue> parseVerdict(Map<String, Object> json) {
        Object valid = json.get("valid");
        if (!(valid instanceof Boolean)) {
            throw new TopicValidationException("INVALID_VERDICT", List.of("valid 는 true 또는 false 여야 합니다"));
        }
        if ((Boolean) valid) {
            return List.of();
        }
        List<TopicIssue> issues = new ArrayList<>();
        for (Map<String, Object> raw : ReportContent.mapList(json, "issues")) {
            String message = trimmed(ReportContent.str(raw, "message"));
            if (message == null) {
                continue;
            }
            if (length(message) > ISSUE_MESSAGE_MAX_LENGTH) {
                message = message.substring(0, message.offsetByCodePoints(0, ISSUE_MESSAGE_MAX_LENGTH));
            }
            String field = trimmed(ReportContent.str(raw, "field"));
            String code = trimmed(ReportContent.str(raw, "code"));
            issues.add(new TopicIssue(
                    field != null && ISSUE_FIELDS.contains(field) ? field : "topic",
                    code != null && ISSUE_CODES.contains(code) ? code : "OTHER",
                    message));
        }
        if (issues.isEmpty()) {
            throw new TopicValidationException("INVALID_VERDICT", List.of("valid 가 false 이면 issues 에 이유를 1개 이상 적어야 합니다"));
        }
        return issues;
    }

    private static TopicContent parseCandidate(Map<String, Object> raw, TopicConditions conditions, String at, List<String> problems) {
        int before = problems.size();
        String question = requireText(raw, "question", QUESTION_MAX_LENGTH, at, problems);
        String description = requireText(raw, "description", DESCRIPTION_MAX_LENGTH, at, problems);

        BattleCategory category = parseCategory(ReportContent.str(raw, "category"));
        if (category == null || !conditions.categories().contains(category)) {
            problems.add(at + ".category 는 " + conditions.categoryNames() + " 중 하나여야 합니다");
        }
        BattleDifficulty difficulty = parseDifficulty(ReportContent.str(raw, "difficulty"));
        if (difficulty != conditions.difficulty()) {
            problems.add(at + ".difficulty 는 " + conditions.difficulty().name() + " 이어야 합니다");
        }

        List<Map<String, Object>> rawOptions = ReportContent.mapList(raw, "options");
        if (rawOptions.size() != 2) {
            problems.add(at + ".options 는 정확히 2개여야 합니다");
            return null;
        }
        List<TopicContent.Option> options = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            String optionAt = at + ".options[" + i + "]";
            Map<String, Object> rawOption = rawOptions.get(i);
            String label = requireText(rawOption, "label", OPTION_LABEL_MAX_LENGTH, optionAt, problems);
            String rationale = requireText(rawOption, "rationale", OPTION_RATIONALE_MAX_LENGTH, optionAt, problems);
            PhilosopherType philosopher = TopicPhilosopherProfiles.parse(ReportContent.str(rawOption, "profile_id"));
            if (philosopher == null) {
                problems.add(optionAt + ".profile_id 는 [철학자 목록]의 id 여야 합니다");
            }
            options.add(new TopicContent.Option(label, rationale, philosopher));
        }
        if (options.get(0).label() != null && options.get(0).label().equals(options.get(1).label())) {
            problems.add(at + ".options 의 두 label 이 같습니다");
        }
        if (options.get(0).philosopher() != null && options.get(0).philosopher() == options.get(1).philosopher()) {
            problems.add(at + ".options 의 두 profile_id 가 같습니다. 서로 다른 철학자를 고르세요");
        }
        if (problems.size() > before) {
            return null;
        }
        return new TopicContent(question, description, category, difficulty, options.get(0), options.get(1));
    }

    private static String requireText(Map<String, Object> raw, String key, int max, String at, List<String> problems) {
        String value = trimmed(ReportContent.str(raw, key));
        if (value == null) {
            problems.add(at + "." + key + " 가 비어 있습니다");
            return null;
        }
        if (length(value) > max) {
            problems.add(at + "." + key + " 는 " + max + "자 이하여야 합니다 (현재 " + length(value) + "자)");
            return null;
        }
        return value;
    }

    private static void checkText(List<TopicIssue> issues, String field, String value, int max, String name) {
        String text = trimmed(value);
        if (text == null) {
            issues.add(new TopicIssue(field, "REQUIRED", name + "을(를) 입력해 주세요."));
        } else if (length(text) > max) {
            issues.add(new TopicIssue(field, "TOO_LONG", name + "은(는) " + max + "자 이하로 적어 주세요. (현재 " + length(text) + "자)"));
        }
    }

    private static BattleCategory parseCategory(String value) {
        if (value == null) {
            return null;
        }
        String v = value.strip();
        for (BattleCategory category : BattleCategory.values()) {
            if (category.name().equals(v) || category.getValue().equals(v)) {
                return category;
            }
        }
        return null;
    }

    private static BattleDifficulty parseDifficulty(String value) {
        if (value == null) {
            return null;
        }
        String v = value.strip();
        for (BattleDifficulty difficulty : BattleDifficulty.values()) {
            if (difficulty.name().equals(v) || ClassTopicPrompts.difficultyLabel(difficulty).equals(v)) {
                return difficulty;
            }
        }
        return null;
    }

    /** 의미 중복 판정의 최소 기준: 공백·문장부호를 빼고 같은 질문 */
    private static String normalize(String question) {
        return question.replaceAll("[\\s\\p{Punct}？！。，]", "");
    }

    static String trimmed(String value) {
        if (value == null) {
            return null;
        }
        String stripped = value.strip();
        return stripped.isEmpty() ? null : stripped;
    }

    static int length(String value) {
        return value.codePointCount(0, value.length());
    }
}

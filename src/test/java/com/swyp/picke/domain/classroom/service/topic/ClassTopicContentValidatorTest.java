package com.swyp.picke.domain.classroom.service.topic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.swyp.picke.domain.battle.enums.BattleCategory;
import com.swyp.picke.domain.battle.enums.BattleDifficulty;
import com.swyp.picke.domain.user.enums.PhilosopherType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ClassTopicContentValidatorTest {

    private final ClassTopicContentValidator validator = new ClassTopicContentValidator();
    private final TopicConditions conditions = new TopicConditions(
            "감정 표현", BattleDifficulty.INTERMEDIATE, List.of(BattleCategory.PHILOSOPHY, BattleCategory.LITERATURE));

    @Test
    @DisplayName("정상 3개는 그대로 받고, 카테고리·수준은 한글 값도 허용한다")
    void acceptsEightCandidates() {
        Map<String, Object> json = json(3);
        candidate(json, 0).put("category", "문학");
        candidate(json, 0).put("difficulty", "중급");

        ClassTopicContentValidator.SuggestResult result = validator.parseSuggestion(json, conditions);

        assertThat(result.needsClarification()).isFalse();
        assertThat(result.candidates()).hasSize(3);
        assertThat(result.candidates().get(0).category()).isEqualTo(BattleCategory.LITERATURE);
        assertThat(result.candidates().get(0).optionB().philosopher()).isEqualTo(PhilosopherType.SARTRE);
    }

    @Test
    @DisplayName("조건 밖 카테고리·길이 초과·같은 철학자·중복 질문은 배치 전체를 거절하고 문제를 모두 모은다")
    void rejectsRuleViolations() {
        Map<String, Object> json = json(5);
        candidate(json, 1).put("category", "SCIENCE");
        candidate(json, 2).put("question", "가".repeat(61));
        option(json, 3, 1).put("profile_id", "KANT");
        candidate(json, 4).put("question", "질문 0 은 무엇일까요?!");

        assertThatThrownBy(() -> validator.parseSuggestion(json, conditions))
                .isInstanceOfSatisfying(TopicValidationException.class, e -> {
                    assertThat(e.getCode()).isEqualTo("CANDIDATE_RULES");
                    assertThat(e.getProblems()).anyMatch(p -> p.startsWith("candidates 는 정확히 3개"));
                    assertThat(e.getProblems()).anyMatch(p -> p.startsWith("candidates[1].category"));
                    assertThat(e.getProblems()).anyMatch(p -> p.startsWith("candidates[2].question") && p.contains("61자"));
                    assertThat(e.getProblems()).anyMatch(p -> p.startsWith("candidates[3].options 의 두 profile_id"));
                    assertThat(e.getProblems()).anyMatch(p -> p.startsWith("candidates[4].question 이 다른 후보와 같습니다"));
                });
    }

    @Test
    @DisplayName("맥락 부족 문구가 비거나 너무 길면 기본 안내로 바꾼다")
    void clarificationFallback() {
        Map<String, Object> json = new HashMap<>();
        json.put("needs_clarification", true);
        json.put("clarification_message", "  ");

        ClassTopicContentValidator.SuggestResult result = validator.parseSuggestion(json, conditions);

        assertThat(result.needsClarification()).isTrue();
        assertThat(result.clarificationMessage()).isEqualTo(ClassTopicContentValidator.DEFAULT_CLARIFICATION);
    }

    @Test
    @DisplayName("판정 응답: valid=false 인데 이유가 없으면 형식 위반, 모르는 field·code 는 일반 값으로")
    void parsesVerdict() {
        assertThat(validator.parseVerdict(Map.of("valid", true, "issues", List.of(Map.of("field", "x", "code", "y", "message", "z")))))
                .isEmpty();
        assertThatThrownBy(() -> validator.parseVerdict(Map.of("valid", false, "issues", List.of())))
                .isInstanceOf(TopicValidationException.class);
        assertThatThrownBy(() -> validator.parseVerdict(Map.of("valid", "yes")))
                .isInstanceOf(TopicValidationException.class);

        List<TopicIssue> issues = validator.parseVerdict(Map.of("valid", false, "issues", List.of(
                Map.of("field", "options[1].label", "code", "OPTIONS_NOT_SAME_AXIS", "message", "같은 축이 아니에요."),
                Map.of("field", "title", "code", "WEIRD", "message", "기타 문제예요."))));
        assertThat(issues).containsExactly(
                new TopicIssue("options[1].label", "OPTIONS_NOT_SAME_AXIS", "같은 축이 아니에요."),
                new TopicIssue("topic", "OTHER", "기타 문제예요."));
    }

    @Test
    @DisplayName("수정안 형식 검사: 필수값·길이·같은 문구·같은 철학자")
    void formatIssues() {
        TopicContent edit = new TopicContent(null, "설명".repeat(41), BattleCategory.PHILOSOPHY, BattleDifficulty.BEGINNER,
                new TopicContent.Option("같다", "이유", PhilosopherType.KANT),
                new TopicContent.Option("같다", "이유", PhilosopherType.KANT));

        assertThat(validator.formatIssues(edit))
                .extracting(TopicIssue::field, TopicIssue::code)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("question", "REQUIRED"),
                        org.assertj.core.groups.Tuple.tuple("description", "TOO_LONG"),
                        org.assertj.core.groups.Tuple.tuple("options[1].label", "DUPLICATE_OPTION"),
                        org.assertj.core.groups.Tuple.tuple("options[1].philosopher", "SAME_PHILOSOPHER"));
    }

    private static Map<String, Object> json(int count) {
        List<Map<String, Object>> candidates = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            candidates.add(new HashMap<>(Map.of(
                    "question", "질문 " + i + "은 무엇일까요?",
                    "description", "설명 " + i,
                    "category", "PHILOSOPHY",
                    "difficulty", "INTERMEDIATE",
                    "options", List.of(
                            new HashMap<>(Map.of("label", "그렇다", "rationale", "이유 A", "profile_id", "KANT")),
                            new HashMap<>(Map.of("label", "아니다", "rationale", "이유 B", "profile_id", "SARTRE"))))));
        }
        Map<String, Object> json = new HashMap<>();
        json.put("needs_clarification", false);
        json.put("candidates", candidates);
        return json;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> candidate(Map<String, Object> json, int index) {
        return ((List<Map<String, Object>>) json.get("candidates")).get(index);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> option(Map<String, Object> json, int index, int option) {
        return ((List<Map<String, Object>>) candidate(json, index).get("options")).get(option);
    }
}

package com.swyp.picke.domain.classroom.service.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * PRD §13 근거 검증: 인용은 본인 발언 원문에 실제로 있어야 하고, 근거 없는 섹션은 null, judgment_basis 근거 없으면 거절.
 */
class ClassReportContentValidatorTest {

    private final ClassReportContentValidator validator = new ClassReportContentValidator();

    private static final MemberFeedbackInput.OptionInfo A = new MemberFeedbackInput.OptionInfo(1L, "선택지 1", "찬성");
    private static final MemberFeedbackInput.OptionInfo B = new MemberFeedbackInput.OptionInfo(2L, "선택지 2", "반대");

    private static MemberFeedbackInput input(int excluded) {
        return new MemberFeedbackInput("제목", "요약", List.of(A, B), A, null, B, List.of(
                new MemberFeedbackInput.Statement("p:10", MemberFeedbackInput.SOURCE_PERSPECTIVE, 10L, LocalDateTime.now(),
                        "반대", "비용 대비 효과가 불분명해서 반대합니다. 데이터가 더 필요해요.", null),
                new MemberFeedbackInput.Statement("c:20", MemberFeedbackInput.SOURCE_COMMENT, 20L, LocalDateTime.now(),
                        "찬성", "그 근거는 표본이 작아 보입니다.", "타인 의견 원문")
        ), excluded);
    }

    private static Map<String, Object> ref(String field, String sourceId, String quote) {
        return Map.of("field", field, "source_id", sourceId, "quote", quote);
    }

    private static Map<String, Object> baseJson() {
        Map<String, Object> json = new HashMap<>();
        json.put("headline", "근거를 먼저 확인하는 판단");
        json.put("judgment_basis", "비용 대비 효과를 기준으로 판단했고 데이터 부족을 이유로 들었어요.");
        json.put("strength", Map.of("title", "근거 점검", "body", "상대 근거의 표본 크기를 짚었어요."));
        json.put("next_focus", Map.of("title", "판단 조건 구체화", "body", "어떤 데이터가 있으면 바뀔지 적어 보세요."));
        json.put("next_question", "어떤 데이터가 있으면 찬성으로 바꿀 수 있나요?");
        json.put("analysis_status", "SUFFICIENT");
        return json;
    }

    @Test
    @DisplayName("본인 발언에 실제로 있는 인용만 근거로 남고, 공백 차이는 무시한다")
    void keepsOnlyValidEvidence() {
        Map<String, Object> json = baseJson();
        json.put("evidence_refs", List.of(
                ref("judgment_basis", "p:10", "비용 대비  효과가 불분명"),   // 공백 차이 → 유효
                ref("strength", "c:20", "표본이 작아"),
                ref("next_focus", "p:10", "원문에 없는 문장"),               // 무효 → next_focus 가 null 이 된다
                ref("strength", "p:99", "비용 대비 효과")                    // 존재하지 않는 발언 → 버림
        ));

        Map<String, Object> content = validator.validateMemberFeedback(json, input(0));

        List<Map<String, Object>> refs = ReportContent.mapList(content, "evidence_refs");
        assertThat(refs).hasSize(2);
        assertThat(refs.get(0)).containsEntry("source_type", "PERSPECTIVE").containsEntry("source_id", 10L);
        assertThat(content.get("strength")).isNotNull();
        assertThat(content.get("next_focus")).isNull();
        assertThat(content).containsEntry("analysis_status", "SUFFICIENT")
                .containsEntry("coverage", "full")
                .containsEntry("pre_option_id", 1L)
                .containsEntry("post_option_id", 2L);
    }

    @Test
    @DisplayName("judgment_basis 에 유효한 근거가 없으면 게시하지 않는다")
    void rejectsWithoutJudgmentEvidence() {
        Map<String, Object> json = baseJson();
        json.put("evidence_refs", List.of(ref("strength", "c:20", "표본이 작아")));

        assertThatThrownBy(() -> validator.validateMemberFeedback(json, input(0)))
                .isInstanceOf(ReportValidationException.class)
                .hasMessage("EVIDENCE_MISSING");
    }

    @Test
    @DisplayName("강점·보완점 모두 근거가 없으면 null 이 되고 analysis_status 는 LIMITED, 제외된 글이 있으면 coverage=partial")
    void limitedWhenNoSectionEvidence() {
        Map<String, Object> json = baseJson();
        json.put("evidence_refs", List.of(ref("judgment_basis", "p:10", "데이터가 더 필요해요")));

        Map<String, Object> content = validator.validateMemberFeedback(json, input(3));

        assertThat(content.get("strength")).isNull();
        assertThat(content.get("next_focus")).isNull();
        assertThat(content).containsEntry("analysis_status", "LIMITED")
                .containsEntry("coverage", "partial")
                .containsEntry("excluded_count", 3);
    }

    @Test
    @DisplayName("길이 규정을 20% 넘게 초과하면 거절한다 (잘라 쓰지 않음)")
    void rejectsTooLong() {
        Map<String, Object> json = baseJson();
        json.put("judgment_basis", "가".repeat(200));
        json.put("evidence_refs", List.of(ref("judgment_basis", "p:10", "데이터가 더 필요해요")));

        assertThatThrownBy(() -> validator.validateMemberFeedback(json, input(0)))
                .isInstanceOf(ReportValidationException.class)
                .hasMessage("TOO_LONG_JUDGMENT_BASIS");
    }

    @Test
    @DisplayName("인사이트는 공개 글 목록에 있는 source_id 만 evidence 1~5, highlight 최대 2로 남긴다")
    void insightFiltersIds() {
        ClassInsightInput input = new ClassInsightInput("제목", null, List.of(A, B), null, List.of(
                new ClassInsightInput.PublicPost("p:1", "PERSPECTIVE", 1L, 1, 1L, "찬성", "내용1", null),
                new ClassInsightInput.PublicPost("p:2", "PERSPECTIVE", 2L, 2, 2L, "반대", "내용2", null),
                new ClassInsightInput.PublicPost("c:3", "COMMENT", 3L, 3, 1L, "찬성", "내용3", "p:1")
        ), 3, 0);
        Map<String, Object> json = Map.of(
                "insight_text", "찬성 쪽은 비용을, 반대 쪽은 효과를 기준으로 삼은 의견이 눈에 띄었어요.",
                "evidence_ids", List.of("p:1", "p:2", "p:2", "p:999", "c:3"),
                "highlight_ids", List.of("p:1", "c:3", "p:2")
        );

        Map<String, Object> insight = validator.validateClassInsight(json, input);

        assertThat(ReportContent.stringList(insight, "evidence_ids")).containsExactly("p:1", "p:2", "c:3");
        assertThat(ReportContent.stringList(insight, "highlight_ids")).containsExactly("p:1", "c:3");
        assertThat(insight).containsEntry("analysis_status", "SUFFICIENT");

        Map<String, Object> noEvidence = Map.of("insight_text", "요약", "evidence_ids", List.of("p:999"));
        assertThatThrownBy(() -> validator.validateClassInsight(noEvidence, input))
                .isInstanceOf(ReportValidationException.class)
                .hasMessage("EVIDENCE_MISSING");
    }

    @Test
    @DisplayName("투표만 한 사람의 템플릿은 선택 변화만 말하고 강점·보완점은 null 이다")
    void limitedTemplate() {
        Map<String, Object> changed = ClassReportContentValidator.limitedMemberFeedback(A, null, B);
        assertThat(changed).containsEntry("judgment_basis", "선택이 선택지 1에서 선택지 2로 바뀌었어요.")
                .containsEntry("headline", "선택의 이유를 확인할 글이 아직 없어요")
                .containsEntry("next_question", "지금의 선택을 한 가장 중요한 이유는 무엇인가요?")
                .containsEntry("analysis_status", "LIMITED");
        assertThat(changed.get("strength")).isNull();
        assertThat(changed.get("next_focus")).isNull();

        Map<String, Object> same = ClassReportContentValidator.limitedMemberFeedback(A, A, A);
        assertThat(same).containsEntry("judgment_basis", "처음부터 끝까지 선택지 1를 선택했어요.")
                .containsEntry("mid_option_id", 1L);
    }

    @Test
    @DisplayName("중간투표가 사전·사후와 다르면 템플릿이 사전→중간→사후 흐름을 말한다")
    void limitedTemplate_withMidVote() {
        Map<String, Object> wavered = ClassReportContentValidator.limitedMemberFeedback(A, B, A);
        assertThat(wavered).containsEntry("judgment_basis", "선택이 선택지 1에서 중간에 선택지 2, 마지막에 선택지 1로 이어졌어요.")
                .containsEntry("pre_option_id", 1L)
                .containsEntry("mid_option_id", 2L)
                .containsEntry("post_option_id", 1L);

        Map<String, Object> noMid = ClassReportContentValidator.limitedMemberFeedback(A, null, B);
        assertThat(noMid).containsEntry("judgment_basis", "선택이 선택지 1에서 선택지 2로 바뀌었어요.");
        assertThat(noMid.get("mid_option_id")).isNull();
    }
}

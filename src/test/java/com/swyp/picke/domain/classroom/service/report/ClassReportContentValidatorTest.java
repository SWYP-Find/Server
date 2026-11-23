package com.swyp.picke.domain.classroom.service.report;

import com.swyp.picke.domain.classroom.dto.response.ClassMidVoteInfo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * PRD §13 근거 검증: 근거는 본인 발언이어야 하고, 인용이 원문과 다르면 원문 앞부분으로 바꾼다.
 * 강점·다음 한 걸음·다음 질문은 항상 채운다 (#681).
 */
class ClassReportContentValidatorTest {

    private final ClassReportContentValidator validator = new ClassReportContentValidator();

    private static final MemberFeedbackInput.OptionInfo A = new MemberFeedbackInput.OptionInfo(1L, "선택지 1", "찬성");
    private static final MemberFeedbackInput.OptionInfo B = new MemberFeedbackInput.OptionInfo(2L, "선택지 2", "반대");

    private static MemberFeedbackInput input(int excluded) {
        return new MemberFeedbackInput("제목", "요약", List.of(A, B), A, List.of(), B, List.of(
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
    @DisplayName("본인 발언만 근거로 남고, 원문과 다른 인용은 버리지 않고 원문 앞부분으로 바꾼다")
    void keepsOwnEvidenceAndReplacesMismatchedQuote() {
        Map<String, Object> json = baseJson();
        json.put("evidence_refs", List.of(
                ref("judgment_basis", "p:10", "비용 대비  효과가 불분명"),   // 공백 차이 → 그대로
                ref("strength", "c:20", "표본이 작아"),
                ref("next_focus", "p:10", "원문에 없는 문장"),               // 원문에 없음 → 원문 앞부분으로
                ref("strength", "p:99", "비용 대비 효과")                    // 존재하지 않는 발언 → 버림
        ));

        Map<String, Object> content = validator.validateMemberFeedback(json, input(0));

        List<Map<String, Object>> refs = ReportContent.mapList(content, "evidence_refs");
        assertThat(refs).hasSize(3);
        assertThat(refs.get(0)).containsEntry("source_type", "PERSPECTIVE").containsEntry("source_id", 10L)
                .containsEntry("quote", "비용 대비  효과가 불분명");
        assertThat(refs.get(2)).containsEntry("field", "next_focus")
                .containsEntry("quote", "비용 대비 효과가 불분명해서 반대합니다. 데이터가 더 필요해요.");
        assertThat(content.get("strength")).isNotNull();
        assertThat(content.get("next_focus")).isNotNull();
        assertThat(content).containsEntry("analysis_status", "SUFFICIENT")
                .containsEntry("coverage", "full")
                .containsEntry("pre_option_id", 1L)
                .containsEntry("post_option_id", 2L);
    }

    @Test
    @DisplayName("근거가 없는 항목은 첫 본인 발언 앞부분을 근거로 붙인다")
    void attachesFallbackEvidence() {
        Map<String, Object> json = baseJson();
        json.put("evidence_refs", List.of(ref("strength", "c:20", "표본이 작아")));

        Map<String, Object> content = validator.validateMemberFeedback(json, input(0));

        List<Map<String, Object>> refs = ReportContent.mapList(content, "evidence_refs");
        assertThat(refs).extracting(r -> r.get("field")).containsExactly("strength", "judgment_basis", "next_focus");
        assertThat(refs.get(1)).containsEntry("source_id", 10L)
                .containsEntry("quote", "비용 대비 효과가 불분명해서 반대합니다. 데이터가 더 필요해요.");
        assertThat(content).containsEntry("analysis_status", "SUFFICIENT");
    }

    @Test
    @DisplayName("인용할 수 있는 본인 발언이 하나도 없으면(모두 가린 비속어) 게시하지 않는다")
    void rejectsWhenNoQuotableStatement() {
        MemberFeedbackInput masked = new MemberFeedbackInput("제목", "요약", List.of(A, B), A, List.of(), B, List.of(
                new MemberFeedbackInput.Statement("p:10", MemberFeedbackInput.SOURCE_PERSPECTIVE, 10L, LocalDateTime.now(),
                        "반대", "*** 반대", null)), 0);
        Map<String, Object> json = baseJson();
        json.put("evidence_refs", List.of(ref("judgment_basis", "p:10", "찬성")));

        assertThatThrownBy(() -> validator.validateMemberFeedback(json, masked))
                .isInstanceOf(ReportValidationException.class)
                .hasMessage("EVIDENCE_MISSING");
    }

    @Test
    @DisplayName("문장부호·따옴표 차이와 말줄임표로 생략한 인용은 원문과 같은 것으로 본다")
    void quoteMatchIgnoresPunctuation() {
        assertThat(ClassReportContentValidator.contains("비용 대비 효과가 불분명해서 반대합니다. 데이터가 더 필요해요.",
                "\"비용 대비 효과가 불분명해서, 반대합니다!\"")).isTrue();
        assertThat(ClassReportContentValidator.contains("비용 대비 효과가 불분명해서 반대합니다. 데이터가 더 필요해요.",
                "비용 대비 효과가… 데이터가 더 필요해요")).isTrue();
        assertThat(ClassReportContentValidator.contains("비용 대비 효과가 불분명해서 반대합니다.", "비용 대비 효과가 분명해서")).isFalse();
    }

    @Test
    @DisplayName("원문 앞부분 인용은 가린 비속어 앞에서 끊고 60자 안에서 자른다")
    void excerptCutsAtMaskAndLength() {
        assertThat(ClassReportContentValidator.excerpt("가격이 너무 비싸서 *** 반대")).isEqualTo("가격이 너무 비싸서");
        assertThat(ClassReportContentValidator.excerpt("*** 몰라")).isNull();
        String longText = "환경을 위해 내는 돈이라면 낼 수 있지만 그 돈이 실제로 어디에 쓰이는지 알려주지 않으면 믿기 어렵고 결국 소비자만 부담하게 됩니다";
        String cut = ClassReportContentValidator.excerpt(longText);
        assertThat(cut).endsWith("…");
        assertThat(cut.length()).isLessThanOrEqualTo(ClassReportContentValidator.EXCERPT_MAX + 1);
        assertThat(longText).startsWith(cut.substring(0, cut.length() - 1));
    }

    @Test
    @DisplayName("AI 가 강점·다음 한 걸음·다음 질문을 비우면 기본 문구로 채우고, 둘 다 비면 LIMITED")
    void fillsDefaultSections() {
        Map<String, Object> json = baseJson();
        json.put("strength", null);
        json.put("next_focus", null);
        json.remove("next_question");
        json.put("evidence_refs", List.of(ref("judgment_basis", "p:10", "데이터가 더 필요해요")));

        Map<String, Object> content = validator.validateMemberFeedback(json, input(0));

        assertThat(content.get("strength")).isEqualTo(Map.of("title", ClassReportContentValidator.DEFAULT_STRENGTH_TITLE,
                "body", ClassReportContentValidator.DEFAULT_STRENGTH_BODY));
        assertThat(content.get("next_focus")).isEqualTo(Map.of("title", ClassReportContentValidator.DEFAULT_NEXT_FOCUS_TITLE,
                "body", ClassReportContentValidator.DEFAULT_NEXT_FOCUS_BODY));
        assertThat(content).containsEntry("next_question", ClassReportContentValidator.DEFAULT_NEXT_QUESTION)
                .containsEntry("analysis_status", "LIMITED");
        assertThat(ReportContent.mapList(content, "evidence_refs")).extracting(r -> r.get("field"))
                .containsExactly("judgment_basis", "strength", "next_focus");
    }

    @Test
    @DisplayName("선택지·입장 문구를 인용하면 그대로 보여주지 않고 본인 발언 원문으로 바꾼다")
    void replacesStanceQuote() {
        Map<String, Object> json = baseJson();
        json.put("evidence_refs", List.of(ref("judgment_basis", "p:10", "선택지 2")));

        Map<String, Object> content = validator.validateMemberFeedback(json, input(0));

        assertThat(ReportContent.mapList(content, "evidence_refs").get(0))
                .containsEntry("quote", "비용 대비 효과가 불분명해서 반대합니다. 데이터가 더 필요해요.");
    }

    @Test
    @DisplayName("AI 가 INSUFFICIENT 를 내면 AI 문장 없이 근거 부족 템플릿(Case A)을 쓴다")
    void insufficientFromAi() {
        Map<String, Object> content = validator.validateMemberFeedback(Map.of("analysis_status", "INSUFFICIENT"), input(0));

        assertThat(content).containsEntry("limited_reason", "INSUFFICIENT_EVIDENCE")
                .containsEntry("headline", "의견 분석을 진행하지 못했어요.")
                .containsEntry("analysis_status", "LIMITED");
    }

    @Test
    @DisplayName("근거 부족 템플릿(Case A)은 PRD 3.3 문구와 선택 기록에 세 항목 안내 문구를 담는다")
    void insufficientTemplate() {
        Map<String, Object> content = ClassReportContentValidator.insufficientMemberFeedback(input(2));

        assertThat(content).containsEntry("headline", "의견 분석을 진행하지 못했어요.")
                .containsEntry("limited_reason", "INSUFFICIENT_EVIDENCE")
                .containsEntry("limited_message", "작성된 의견에서 주제와 관련된 주장이나 근거를 충분히 확인하기 어려워 사고 과정 분석을 제공하지 않습니다.")
                .containsEntry("limited_notice", "투표 결과와 참여 기록은 정상적으로 확인할 수 있습니다.")
                .containsEntry("judgment_basis", "선택이 선택지 1에서 선택지 2로 바뀌었어요.")
                .containsEntry("analysis_status", "LIMITED")
                .containsEntry("coverage", "partial")
                .containsEntry("excluded_count", 2)
                .containsEntry("strength", Map.of("title", ClassReportContentValidator.TEMPLATE_STRENGTH_TITLE,
                        "body", ClassReportContentValidator.TEMPLATE_STRENGTH_BODY))
                .containsEntry("next_focus", Map.of("title", ClassReportContentValidator.TEMPLATE_NEXT_FOCUS_TITLE,
                        "body", ClassReportContentValidator.TEMPLATE_NEXT_FOCUS_BODY))
                .containsEntry("next_question", ClassReportContentValidator.TEMPLATE_NEXT_QUESTION)
                .containsEntry("evidence_refs", List.of());
    }

    @Test
    @DisplayName("강점·보완점에 근거가 없어도 AI 문장은 남기고 근거를 붙인다. 제외된 글이 있으면 coverage=partial")
    void keepsSectionsWithoutEvidence() {
        Map<String, Object> json = baseJson();
        json.put("evidence_refs", List.of(ref("judgment_basis", "p:10", "데이터가 더 필요해요")));

        Map<String, Object> content = validator.validateMemberFeedback(json, input(3));

        assertThat(content.get("strength")).isEqualTo(Map.of("title", "근거 점검", "body", "상대 근거의 표본 크기를 짚었어요."));
        assertThat(content.get("next_focus")).isNotNull();
        assertThat(content).containsEntry("analysis_status", "SUFFICIENT")
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
                new ClassInsightInput.PublicPost("p:1", "PERSPECTIVE", 1L, 1, 1L, "찬성", "비용보다 기대 효과가 커서 찬성해요.", null),
                new ClassInsightInput.PublicPost("p:2", "PERSPECTIVE", 2L, 2, 2L, "반대", "표본이 작아서 효과를 믿기 어려워요.", null),
                new ClassInsightInput.PublicPost("c:3", "COMMENT", 3L, 3, 1L, "찬성", "장기적으로 보면 비용도 줄어들 거예요.", "p:1")
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

    private static ClassInsightInput.PublicPost post(String id, int author, String content) {
        return new ClassInsightInput.PublicPost(id, id.startsWith("p:") ? "PERSPECTIVE" : "COMMENT",
                Long.parseLong(id.substring(2)), author, 1L, "찬성", content, null);
    }

    private static ClassInsightInput insightInput(ClassInsightInput.PublicPost... posts) {
        return new ClassInsightInput("제목", null, List.of(A, B), null, List.of(posts), posts.length, 0);
    }

    @Test
    @DisplayName("#657 AI 가 excluded_ids 로 표시한 글과 비속어·이유 없는 글은 근거·하이라이트에서 빠진다")
    void insightDropsExcludedAndUnselectablePosts() {
        ClassInsightInput input = insightInput(
                post("p:1", 1, "비용보다 기대 효과가 커서 찬성해요."),
                post("p:2", 2, "표본이 작아서 효과를 믿기 어려워요."),
                post("p:3", 3, "장기적으로 보면 비용도 줄어들 거예요."),
                post("p:4", 4, "민수야 너 맨날 그러잖아 그래서 찬성하는 거지"),
                post("p:5", 5, "씨발 이딴 거 왜 함 당연히 반대지"),
                post("p:6", 6, "ㅋㅋㅋㅋㅋㅋㅋㅋ"));
        Map<String, Object> json = Map.of(
                "excluded_ids", List.of("p:4"),
                "insight_text", "찬성 쪽은 비용을, 반대 쪽은 효과를 기준으로 삼았어요.",
                "evidence_ids", List.of("p:1", "p:4", "p:5", "p:6", "p:2"),
                "highlight_ids", List.of("p:5", "p:4", "p:3"));

        Map<String, Object> insight = validator.validateClassInsight(json, input);

        assertThat(ReportContent.stringList(insight, "evidence_ids")).containsExactly("p:1", "p:2");
        assertThat(ReportContent.stringList(insight, "highlight_ids")).containsExactly("p:3");
        assertThat(insight).containsEntry("analysis_status", "SUFFICIENT");
    }

    @Test
    @DisplayName("#657 쓸 수 있는 글의 작성자가 3명 미만이면 AI 요약 대신 의견 부족 템플릿(LIMITED)을 쓴다")
    void insightLimitedWhenUsableAuthorsBelowThree() {
        ClassInsightInput input = insightInput(
                post("p:1", 1, "AI가 초안을 써도 학생이 검토하고 고치면 학습이라고 생각해요."),
                post("p:2", 2, "아무거나 쓰면 되죠 뭐"),
                post("p:3", 3, "배고프다 집에 가고 싶다"),
                post("p:4", 4, "qwerasdfzxcv"));
        Map<String, Object> json = Map.of(
                "excluded_ids", List.of("p:3", "p:4"),
                "insight_text", "찬성 쪽은 창의성을 반영할 수 있다고 주장했어요.",
                "evidence_ids", List.of("p:1", "p:2"),
                "highlight_ids", List.of("p:2"));

        Map<String, Object> insight = validator.validateClassInsight(json, input);

        assertThat(insight).containsEntry("analysis_status", "LIMITED")
                .containsEntry("insight_text", "판단의 이유를 살펴볼 글이 아직 충분하지 않아요");
        assertThat(ReportContent.stringList(insight, "evidence_ids")).isEmpty();
    }

    @Test
    @DisplayName("#657 AI 가 INSUFFICIENT 를 내면 요약 없이 의견 부족 템플릿(LIMITED)을 쓴다")
    void insightInsufficient() {
        ClassInsightInput input = insightInput(
                post("p:1", 1, "아무거나 쓰면 되죠 뭐"),
                post("p:2", 2, "배고프다 집에 가고 싶다"),
                post("p:3", 3, "오늘 날씨 진짜 좋네요"));

        Map<String, Object> insight = validator.validateClassInsight(Map.of("analysis_status", "INSUFFICIENT"), input);

        assertThat(insight).containsEntry("analysis_status", "LIMITED")
                .containsEntry("insight_text", "판단의 이유를 살펴볼 글이 아직 충분하지 않아요");
    }

    @Test
    @DisplayName("투표만 한 사람의 템플릿(Case B)은 선택 변화를 말하고 세 항목은 안내 문구로 채운다")
    void limitedTemplate() {
        Map<String, Object> changed = ClassReportContentValidator.limitedMemberFeedback(A, null, B);
        assertThat(changed).containsEntry("judgment_basis", "선택이 선택지 1에서 선택지 2로 바뀌었어요.")
                .containsEntry("headline", "작성된 의견이 없어요.")
                .containsEntry("limited_reason", "NO_POST")
                .containsEntry("limited_message", "배틀에 참여했지만 작성된 의견이 없어 사고 과정 분석을 제공하지 않습니다.")
                .containsEntry("limited_notice", "참여 여부와 투표 결과는 확인할 수 있습니다.")
                .containsEntry("next_question", ClassReportContentValidator.TEMPLATE_NEXT_QUESTION)
                .containsEntry("analysis_status", "LIMITED");
        assertThat(changed.get("strength")).isNotNull();
        assertThat(changed.get("next_focus")).isNotNull();

        Map<String, Object> same = ClassReportContentValidator.limitedMemberFeedback(A, List.of(), A);
        assertThat(same).containsEntry("judgment_basis", "처음부터 끝까지 선택지 1를 선택했어요.")
                .containsEntry("mid_choices", List.of());
    }

    @Test
    @DisplayName("중간투표가 있으면 템플릿이 콘텐츠 중 고른 문구를 덧붙이고 지점별로 저장한다")
    void limitedTemplate_withMidVote() {
        List<ClassMidVoteInfo> mid = List.of(new ClassMidVoteInfo(31L, 41L, "유죄"), new ClassMidVoteInfo(32L, 43L, "무죄"));
        Map<String, Object> wavered = ClassReportContentValidator.limitedMemberFeedback(A, mid, A);
        assertThat(wavered).containsEntry("judgment_basis", "처음부터 끝까지 선택지 1를 선택했어요. 콘텐츠 중간에는 '유죄', '무죄' 쪽을 골랐어요.")
                .containsEntry("pre_option_id", 1L)
                .containsEntry("post_option_id", 1L);
        assertThat((List<?>) wavered.get("mid_choices")).hasSize(2);

        Map<String, Object> noMid = ClassReportContentValidator.limitedMemberFeedback(A, null, B);
        assertThat(noMid).containsEntry("judgment_basis", "선택이 선택지 1에서 선택지 2로 바뀌었어요.");
        assertThat((List<?>) noMid.get("mid_choices")).isEmpty();
    }
}

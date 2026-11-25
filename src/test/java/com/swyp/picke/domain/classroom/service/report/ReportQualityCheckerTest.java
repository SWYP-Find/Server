package com.swyp.picke.domain.classroom.service.report;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 개인 리포트 품질 검사 (#707). 11/24 QA 클래스 34 사례로 만든다 */
class ReportQualityCheckerTest {

    private static final MemberFeedbackInput.OptionInfo TECH = new MemberFeedbackInput.OptionInfo(1L, "기술 우선",
            "개인의 결심에만 기대는 해법에는 한계가 있으므로, 기술과 생산 시스템의 기반부터 바꿔야 한다.");
    private static final MemberFeedbackInput.OptionInfo HABIT = new MemberFeedbackInput.OptionInfo(2L, "습관 우선",
            "기술이 환경 부담을 줄여주어도 인간의 욕심이 계속 커지면 제자리걸음이므로, 불필요한 소비 습관을 덜어내는 것이 먼저다.");

    private static MemberFeedbackInput input(String... statements) {
        List<MemberFeedbackInput.Statement> list = new java.util.ArrayList<>();
        for (int i = 0; i < statements.length; i++) {
            list.add(new MemberFeedbackInput.Statement("p:" + i, MemberFeedbackInput.SOURCE_PERSPECTIVE, (long) i,
                    LocalDateTime.now(), "습관 우선", statements[i], null));
        }
        return new MemberFeedbackInput("기후위기", "진짜 필요한 것만 신중히 골라 오래 쓰는 습관이 해법이라는 노자와 기술을 앞세우는 마르크스가 맞붙는다",
                List.of(TECH, HABIT), HABIT, List.of(), HABIT, list, 0);
    }

    private static Map<String, Object> report(String basis, String strengthTitle, String strengthBody, String focusTitle, String focusBody) {
        Map<String, Object> content = new HashMap<>();
        content.put("headline", "습관 개선이 먼저라는 판단");
        content.put("judgment_basis", basis);
        content.put("strength", Map.of("title", strengthTitle, "body", strengthBody));
        content.put("next_focus", Map.of("title", focusTitle, "body", focusBody));
        content.put("next_question", "기술이 먼저 바뀌어야 하는 경우는 언제일까요?");
        return content;
    }

    private static final String OWN = "아무리 기술을 빠르게 발전시키고자해도 인간의 소비 속도를 따라올 순 없어요. 습관 개선을 우선해야 합니다.";

    @Test
    @DisplayName("참가자 글에 없는 선택지 문장을 참가자 주장으로 쓰면 걸리고, 참가자가 실제로 쓴 구절이면 통과한다")
    void unsupportedOptionText() {
        Map<String, Object> copied = report(
                "기술이 환경 부담을 줄여도 인간의 욕심이 계속 커지면 제자리걸음이라는 점을 강조했어요.",
                "소비 속도에 주목했어요", "기술 발전이 소비 속도를 따라오지 못한다는 점을 짚었어요.",
                "기술 쪽 반론 다뤄 보기", "기술 우선 쪽에서 나올 반론에 답해 보면 좋겠어요.");
        assertThat(ReportQualityChecker.memberFeedbackIssues(copied, input(OWN))).contains("UNSUPPORTED_OPTION_TEXT");

        // 같은 문장이라도 참가자가 실제로 비슷하게 썼다면 문제 아님
        String ownSimilar = "기술이 환경 부담을 줄여도 인간의 욕심이 계속 커지면 제자리걸음이라고 생각해요.";
        assertThat(ReportQualityChecker.memberFeedbackIssues(copied, input(ownSimilar))).doesNotContain("UNSUPPORTED_OPTION_TEXT");
    }

    @Test
    @DisplayName("관찰 항목 이름을 제목으로 그대로 쓰면 GENERIC_TITLE")
    void genericTitle() {
        Map<String, Object> content = report("소비 속도를 기준으로 습관 개선을 골랐어요.", "주장과 이유의 연결",
                "기술 발전보다 소비 속도가 빠르다는 근거를 들었어요.", "반대 논점에 대한 응답", "반론에 답해 보면 좋겠어요.");
        assertThat(ReportQualityChecker.memberFeedbackIssues(content, input(OWN))).contains("GENERIC_TITLE");
        assertThat(ReportQualityChecker.isCategoryName("소비 속도로 근거를 댔어요")).isFalse();
    }

    @Test
    @DisplayName("잘한 점이 판단 기준을 거의 그대로 반복하면 REDUNDANT_STRENGTH, '~습니다' 가 섞이면 TONE")
    void redundantAndTone() {
        String basis = "인간의 발전을 위해 동물 실험이 필요하다는 주장을 하면서도, 고통을 최소화해야 한다는 조건을 고려하고 있어요.";
        Map<String, Object> content = report(basis, "필요성과 조건을 함께 봤어요",
                "인간의 발전을 위한 동물 실험이 필요하다는 주장을 하면서, 동물 실험을 최소화해야 한다고 언급했어요.",
                "대안 찾아 보기", "동물 실험 외의 대안을 탐색해 보세요. 구체적으로 정리하면 좋겠습니다.");
        List<String> issues = ReportQualityChecker.memberFeedbackIssues(content, input("동물 실험을 최소화해야 한다고 생각합니다"));
        assertThat(issues).contains("REDUNDANT_STRENGTH", "TONE");
    }

    @Test
    @DisplayName("문제없는 리포트와 서버 템플릿은 걸리지 않는다")
    void cleanAndTemplate() {
        Map<String, Object> clean = report("소비 속도를 기준으로 습관 개선을 골랐어요.", "인구 수로 규모를 짚었어요",
                "80억이라는 숫자로 기술만으로는 부족하다는 점을 보여줬어요.", "기술의 역할도 따져 보기",
                "기술이 습관 변화를 돕는 경우도 있는지 살펴보면 좋겠어요.");
        assertThat(ReportQualityChecker.memberFeedbackIssues(clean, input(OWN))).isEmpty();

        Map<String, Object> template = ClassReportContentValidator.limitedMemberFeedback(HABIT, null, HABIT);
        assertThat(ReportQualityChecker.memberFeedbackIssues(template, input(OWN))).isEmpty();
    }

    @Test
    @DisplayName("배틀 요약 문장을 참가자 주장으로 옮겨 써도 걸린다 (선택지 입장이 빈 배틀, 11/24 QA 클래스 33)")
    void unsupportedSummaryText() {
        Map<String, Object> content = report("진짜 필요한 것만 신중히 골라 오래 쓰는 습관을 강조했어요.", "소비 속도에 주목했어요",
                "기술 발전이 소비 속도를 따라오지 못한다는 점을 짚었어요.", "기술 쪽 반론 다뤄 보기", "기술 우선 쪽 반론에 답해 보면 좋겠어요.");
        assertThat(ReportQualityChecker.memberFeedbackIssues(content, input(OWN))).contains("UNSUPPORTED_OPTION_TEXT");
    }

    @Test
    @DisplayName("주제어 수준의 짧은 겹침('인간의 발전을 위해')은 참가자가 안 썼어도 통과한다 (#711)")
    void topicLevelOverlapPasses() {
        MemberFeedbackInput.OptionInfo allow = new MemberFeedbackInput.OptionInfo(1L, "허용된다", "인간의 발전을 위해 필요하다.");
        MemberFeedbackInput.Statement own = new MemberFeedbackInput.Statement("p:1", MemberFeedbackInput.SOURCE_PERSPECTIVE, 1L,
                LocalDateTime.now(), "허용된다", "신약 안전성을 확인하려면 아직 다른 방법이 부족해요.", null);
        MemberFeedbackInput input = new MemberFeedbackInput("동물 실험", "동물 실험의 윤리적 정당성을 논의합니다.", List.of(allow), allow,
                List.of(), allow, List.of(own), 0);
        assertThat(ReportQualityChecker.hasUnsupportedOptionText(
                "인간의 발전을 위해 신약 안전성 확인이 필요하다고 봤어요.", input)).isFalse();
    }

    @Test
    @DisplayName("다음 생각 한 걸음 질문이 '~생각했어요?' 과거형이면 QUESTION_TENSE, 현재형은 통과 (#717)")
    void questionTense() {
        Map<String, Object> content = report("소비 속도를 기준으로 습관 개선을 골랐어요.", "인구 수로 규모를 짚었어요",
                "80억이라는 숫자로 기술만으로는 부족하다는 점을 보여줬어요.", "기술의 역할도 따져 보기",
                "기술이 습관 변화를 돕는 경우도 있는지 살펴보면 좋겠어요.");
        content.put("next_question", "어떤 조건을 넘으면 허용하지 말아야 한다고 생각했어요?");
        assertThat(ReportQualityChecker.memberFeedbackIssues(content, input(OWN))).containsExactly("QUESTION_TENSE");
        content.put("next_question", "어떤 조건을 넘으면 허용하지 말아야 할까요?");
        assertThat(ReportQualityChecker.memberFeedbackIssues(content, input(OWN))).isEmpty();
    }
}

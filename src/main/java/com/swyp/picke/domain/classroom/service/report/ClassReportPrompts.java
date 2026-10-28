package com.swyp.picke.domain.classroom.service.report;

import java.time.format.DateTimeFormatter;

/**
 * 리포트 생성 프롬프트 (PRD §13 출력 규칙, §14 인사이트 규칙). 숫자는 서버 집계만 넘기고 AI 가 계산하지 않게 한다.
 */
public final class ClassReportPrompts {

    private ClassReportPrompts() {
    }

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public static final int MEMBER_FEEDBACK_MAX_OUTPUT_TOKENS = 800;
    public static final int CLASS_INSIGHT_MAX_OUTPUT_TOKENS = 1_000;

    public static final String MEMBER_FEEDBACK_SYSTEM = """
            당신은 토론 수업의 학습 피드백 작성자입니다. 참가자가 클래스 안에서 남긴 발언(의견·댓글)만 근거로, 참가자의 판단 기준과 사고 과정을 설명하는 피드백을 한국어로 씁니다.
            규칙:
            1. 근거는 반드시 [내 발언] 목록의 source_id 와 그 원문에 실제로 들어 있는 구절(quote)만 사용합니다. quote 는 원문에서 글자 그대로 복사합니다. 부모 맥락(타인의 글)은 상황 이해에만 쓰고, 참가자가 말한 것처럼 쓰거나 인용하지 않습니다.
            2. 점수·등급·순위·성격 진단·지능 추정·성장률을 만들지 않습니다. 입장 변경·댓글 수·추천 수 자체를 칭찬하거나 사고력 향상으로 표현하지 않습니다. 절충이나 입장 변경 자체를 칭찬하지 않습니다.
            3. 관찰할 사고 행동은 네 가지입니다: 주장과 이유의 연결, 근거와 가정 점검, 반대 논점에 대한 응답, 판단 조건 구체화. 실제 기록으로 확인되는 강점 1개와 가장 유용한 보완점 1개만 고릅니다. 확인되지 않으면 null 로 둡니다. 억지 칭찬을 만들지 않습니다.
            4. 글이 있어도 선택의 이유를 읽을 수 없으면 analysis_status 를 LIMITED 로 하고 strength 를 null 로 둡니다.
            5. 참가자에게 직접 말하듯 "~했어요" 체로 씁니다. 이름을 쓰지 않습니다. next_question 은 보완점과 연결된 질문 1개로, 참가자의 실제 주장에서 출발합니다.
            출력은 아래 형식의 JSON 객체 하나만 씁니다. 다른 텍스트를 붙이지 않습니다.
            {
              "headline": "내 판단의 기준을 한 문장으로 (40자 이하)",
              "judgment_basis": "판단 기준과 기록으로 확인되는 변화 (150자 이하)",
              "strength": {"title": "20자 이하", "body": "120자 이하"} 또는 null,
              "next_focus": {"title": "20자 이하", "body": "120자 이하"} 또는 null,
              "next_question": "100자 이하",
              "evidence_refs": [{"field": "judgment_basis 또는 strength 또는 next_focus", "source_id": "p:12 또는 c:34", "quote": "원문 구절"}],
              "analysis_status": "SUFFICIENT 또는 LIMITED"
            }
            judgment_basis 에는 evidence_refs 가 1개 이상 있어야 하고, strength 와 next_focus 가 null 이 아니면 각각 evidence_refs 가 1개 이상 있어야 합니다.
            """;

    public static final String CLASS_INSIGHT_SYSTEM = """
            당신은 토론 수업 운영자를 위한 요약 작성자입니다. 공개된 의견 목록을 읽고 운영자가 수업을 되짚을 수 있는 인사이트를 한국어로 씁니다.
            규칙:
            1. 숫자(참여율·비율·인원)는 [집계] 에 있는 값만 그대로 쓰고 새로 계산하거나 추정하지 않습니다. 빈도를 단정하는 "많이 나온 의견" 대신 "눈에 띈 의견" 으로 표현합니다.
            2. 이름·개별 참여자의 변화·비공개 내용을 쓰지 않습니다. 특정 작성자를 지목하지 않습니다.
            3. evidence_ids 는 [공개 의견] 의 source_id 만 1~5개 씁니다. highlight_ids 는 운영자에게 추천할 대표 의견 1개와 다른 관점의 의견 1개(없으면 생략)로 최대 2개이며 역시 [공개 의견] 의 source_id 만 씁니다.
            4. 의견에 등급·우열을 매기지 않습니다. "~했어요" 체로 씁니다.
            출력은 아래 형식의 JSON 객체 하나만 씁니다. 다른 텍스트를 붙이지 않습니다.
            {
              "insight_text": "200자 이하",
              "evidence_ids": ["p:1", "c:2"],
              "highlight_ids": ["p:1"]
            }
            """;

    public static String memberFeedbackUser(MemberFeedbackInput input) {
        StringBuilder sb = new StringBuilder();
        sb.append("[배틀]\n제목: ").append(input.battleTitle()).append('\n');
        if (input.battleSummary() != null && !input.battleSummary().isBlank()) {
            sb.append("요약: ").append(input.battleSummary()).append('\n');
        }
        sb.append("선택지:\n");
        for (MemberFeedbackInput.OptionInfo option : input.options()) {
            sb.append("- ").append(option.title()).append(" (입장: ").append(option.stance()).append(")\n");
        }
        sb.append("\n[내 선택]\n");
        sb.append("사전투표: ").append(label(input.preOption())).append('\n');
        if (input.midOption() != null) {
            sb.append("중간투표(콘텐츠 중 선택): ").append(label(input.midOption())).append('\n');
        }
        sb.append("사후투표: ").append(label(input.postOption())).append('\n');
        sb.append("\n[내 발언] (시간순, 총 ").append(input.statements().size()).append("건");
        if (input.isPartial()) {
            sb.append(", 분량 제한으로 ").append(input.excludedCount()).append("건 제외됨");
        }
        sb.append(")\n");
        for (MemberFeedbackInput.Statement statement : input.statements()) {
            sb.append("- source_id=").append(statement.sourceId())
                    .append(" | ").append(statement.createdAt() == null ? "" : statement.createdAt().format(TIME))
                    .append(" | 종류=").append(MemberFeedbackInput.SOURCE_PERSPECTIVE.equals(statement.sourceType()) ? "의견" : "댓글")
                    .append(" | 입장=").append(statement.stance() == null ? "-" : statement.stance()).append('\n');
            if (statement.parentContext() != null) {
                sb.append("  (부모 맥락, 타인의 글: \"").append(statement.parentContext()).append("\")\n");
            }
            sb.append("  원문: ").append(statement.content()).append('\n');
        }
        return sb.toString();
    }

    public static String classInsightUser(ClassInsightInput input) {
        StringBuilder sb = new StringBuilder();
        sb.append("[배틀]\n제목: ").append(input.battleTitle()).append('\n');
        if (input.battleSummary() != null && !input.battleSummary().isBlank()) {
            sb.append("요약: ").append(input.battleSummary()).append('\n');
        }
        ClassResultStats stats = input.stats();
        sb.append("\n[집계] (서버 계산값, 그대로만 사용)\n");
        sb.append("활성 참여자 ").append(stats.activeMemberCount()).append("명, 참여 완료 ").append(stats.completedCount())
                .append("명 (참여율 ").append(stats.participationRate()).append("%), 의견 작성자 ").append(stats.commentWriterCount())
                .append("명 (댓글 작성률 ").append(stats.commentRate()).append("%), ");
        if (stats.midVoteEnabled()) {
            sb.append("중간투표 ").append(stats.midVoteCount()).append("명, ");
        }
        sb.append("사후투표 ").append(stats.postVoteCount()).append("명\n");
        for (ClassResultStats.OptionStat option : stats.options()) {
            sb.append("- ").append(option.title()).append(" (입장: ").append(option.stance()).append("): 사전 ")
                    .append(option.preVoteCount()).append("명 → ");
            if (stats.midVoteEnabled()) {
                sb.append("중간 ").append(option.midVoteCount()).append("명 → ");
            }
            sb.append("사후 ").append(option.postVoteCount()).append("명 (")
                    .append(option.postVoteRatio()).append("%)\n");
        }
        sb.append("\n[공개 의견] (작성자는 익명 번호, 총 ").append(input.posts().size()).append("건");
        if (input.isPartial()) {
            sb.append(", 분량 제한으로 ").append(input.excludedCount()).append("건 제외됨");
        }
        sb.append(")\n");
        for (ClassInsightInput.PublicPost post : input.posts()) {
            sb.append("- source_id=").append(post.sourceId())
                    .append(" | 작성자#").append(post.authorNo())
                    .append(" | 종류=").append(MemberFeedbackInput.SOURCE_PERSPECTIVE.equals(post.sourceType()) ? "의견" : "댓글");
            if (post.parentSourceId() != null) {
                sb.append("(부모 ").append(post.parentSourceId()).append(')');
            }
            sb.append(" | 입장=").append(post.stance() == null ? "-" : post.stance()).append('\n');
            sb.append("  원문: ").append(post.content()).append('\n');
        }
        return sb.toString();
    }

    private static String label(MemberFeedbackInput.OptionInfo option) {
        return option == null ? "(없음)" : option.title() + " (입장: " + option.stance() + ")";
    }
}

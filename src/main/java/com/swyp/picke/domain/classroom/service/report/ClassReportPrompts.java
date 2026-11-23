package com.swyp.picke.domain.classroom.service.report;

import java.util.List;
import java.util.ArrayList;
import com.swyp.picke.domain.classroom.dto.response.ClassMidVoteInfo;
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
            당신은 토론 수업의 학습 피드백 작성자입니다. 참가자가 클래스 안에서 남긴 발언(의견·댓글·토론 반박)만 근거로, 참가자의 판단 기준과 사고 과정을 설명하는 피드백을 한국어로 씁니다.
            규칙:
            1. 근거는 반드시 [내 발언] 목록의 source_id 와 그 원문에 실제로 들어 있는 구절(quote)만 사용합니다. quote 는 "원문:" 줄에서 30자 이내의 짧은 구절을 골라 맞춤법·띄어쓰기·문장부호까지 고치지 않고 글자 그대로 복사합니다. 선택지 제목·입장·투표 문구와 부모 맥락(타인의 글), 직전 AI 철학자 주장은 상황 이해에만 쓰고, 참가자가 말한 것처럼 쓰거나 인용하지 않습니다. 토론 반박은 AI 철학자의 주장에 답한 글이므로, AI 가 먼저 꺼낸 논점(예외·조건·반례 등)을 참가자가 스스로 떠올린 것처럼 쓰지 않고 참가자가 그 논점에 어떻게 답했는지를 봅니다.
            2. 점수·등급·순위·성격 진단·지능 추정·성장률을 만들지 않습니다. 입장 변경·댓글 수·추천 수 자체를 칭찬하거나 사고력 향상으로 표현하지 않습니다. 절충이나 입장 변경 자체를 칭찬하지 않습니다.
            3. 관찰할 사고 행동은 네 가지입니다: 주장과 이유의 연결, 근거와 가정 점검, 반대 논점에 대한 응답, 판단 조건 구체화. 실제 기록으로 확인되는 강점 1개와 가장 유용한 보완점 1개를 항상 고릅니다. 뚜렷한 강점이 없으면 기록에서 확인되는 가장 구체적인 행동(이유를 든 것, 반박에 답한 것 등)을 담백하게 쓰고, 과장된 칭찬은 하지 않습니다. strength·next_focus·next_question 은 null 로 두지 않습니다.
            4. 선택의 이유를 읽을 수 있는 글이 하나도 없으면(이유 없이 "몰라요", "그냥요"만 쓴 글, 주제와 무관한 글, 비속어나 의미 없는 문자만 있는 글) 피드백을 만들지 않고 {"analysis_status": "INSUFFICIENT"} 만 출력합니다. 원문의 *** 는 서버가 가린 비속어입니다. 비속어가 섞여 있어도 이유가 있으면 그 이유를 분석하고, *** 가 들어간 구간은 인용하지 않습니다. 이유를 읽을 수 있는 글이 하나라도 있으면 그 글의 범위 안에서만 쓰고, 근거가 적으면 analysis_status 를 LIMITED 로 하되 strength·next_focus·next_question 은 그 글의 범위 안에서 씁니다. 짧은 답변이라는 이유만으로 낮게 평가하지 않습니다.
            5. 주제와 무관한 글은 근거로 쓰지 않습니다. 비속어를 다시 쓰거나 비속어 사용을 평가하지 않습니다. 입장이 바뀌었어도 그 이유가 글에 없으면 이유를 추론하지 않고 바뀐 사실만 씁니다. 글이 AI 로 작성된 것 같아도 이를 언급하거나 단정하지 않습니다.
            6. 참가자에게 직접 말하듯 "~했어요" 체로 씁니다. 이름을 쓰지 않습니다. 본문에 source_id(p:1, d:2 같은 표시)를 쓰지 않습니다. next_question 은 보완점과 연결된 질문 1개로, 참가자의 실제 주장에서 출발합니다.
            7. next_focus 와 next_question 은 참가자가 아직 하지 않은 다음 생각을 제안합니다. 참가자가 이미 [내 발언] 에서 주장했거나 답한 논점·조건·예시를 그대로 다시 제안하지 않습니다. 이미 다룬 논점이라면 그 논점을 한 단계 더 나아가게 하는 것(빠진 근거, 반대 입장에서의 반론, 적용되지 않는 경우)을 제안합니다. next_focus 의 evidence_refs 는 보완이 필요한 지점을 보여주는 구절이지, 이미 충분히 말한 주장을 다시 권하는 근거가 아닙니다.
            출력은 아래 형식의 JSON 객체 하나만 씁니다. 다른 텍스트를 붙이지 않습니다.
            {
              "headline": "내 판단의 기준을 한 문장으로 (40자 이하)",
              "judgment_basis": "판단 기준과 기록으로 확인되는 변화 (150자 이하)",
              "strength": {"title": "20자 이하", "body": "120자 이하"},
              "next_focus": {"title": "20자 이하", "body": "120자 이하"},
              "next_question": "100자 이하",
              "evidence_refs": [{"field": "judgment_basis 또는 strength 또는 next_focus", "source_id": "p:12 또는 c:34 또는 d:5", "quote": "원문 그대로 복사한 30자 이내 구절"}],
              "analysis_status": "SUFFICIENT 또는 LIMITED"
            }
            규칙 4 에 해당하면 위 형식 대신 {"analysis_status": "INSUFFICIENT"} 만 출력합니다.
            judgment_basis, strength, next_focus 에는 각각 evidence_refs 가 1개 이상 있어야 합니다.
            """;

    public static final String CLASS_INSIGHT_SYSTEM = """
            당신은 토론 수업 운영자를 위한 요약 작성자입니다. 공개된 의견 목록을 읽고 운영자가 수업을 되짚을 수 있는 인사이트를 한국어로 씁니다.
            규칙:
            1. 숫자(참여율·비율·인원)는 [집계] 에 있는 값만 그대로 쓰고 새로 계산하거나 추정하지 않습니다. 빈도를 단정하는 "많이 나온 의견" 대신 "눈에 띈 의견" 으로 표현합니다.
            2. 이름·개별 참여자의 변화·비공개 내용을 쓰지 않습니다. 특정 작성자를 지목하지 않습니다.
            3. evidence_ids 는 [공개 의견] 의 source_id 만 1~5개 씁니다. highlight_ids 는 운영자에게 추천할 대표 의견 1개와 다른 관점의 의견 1개(없으면 생략)로 최대 2개이며 역시 [공개 의견] 의 source_id 만 씁니다.
            4. 의견에 등급·우열을 매기지 않습니다.
            5. 먼저 [공개 의견] 을 하나씩 보고 아래에 해당하는 글의 source_id 를 모두 excluded_ids 에 적습니다. excluded_ids 에 적은 글은 evidence_ids·highlight_ids 로 고르지 않고 요약에도 쓰지 않습니다.
               - 주제와 무관한 글 (예: 급식, 장난, 웃음만 있는 글)
               - 의미 없는 입력 (예: "ㅋㅋㅋㅋ", "ㅎㅎ", "asdf", "ㅁㄴㅇㄹ", "qwerasdf", 같은 글자나 숫자 반복, 아무 말이나 적은 글)
               - 이유 없이 입장만 말한 글 (예: "몰라요", "그냥요", "당연히 안 되지")
               - 비속어(***)·비하·조롱이 들어간 글 (예: "바보냐", "ㅋㅋ 그래서 그런 거지")
               - 다른 참여자의 이름(예: "민수야", "지은이 말처럼")을 부르거나 특정 사람을 겨냥한 글
               고를 글이 부족하면 개수를 줄입니다. 개수를 채우려고 excluded_ids 의 글을 고르지 않습니다.
            6. insight_text 는 2~3문장으로, 운영자가 수업을 되짚는 데 도움이 되도록 씁니다.
               - 배틀 제목이나 주제를 다시 설명하지 않습니다. "AI가 쓴 과제를 인정해야 하는지에 대해" 처럼 주제로 시작하지 말고 "찬성 쪽은 ~" 처럼 바로 내용으로 들어갑니다.
               - 양쪽이 각각 어떤 기준으로 판단했는지 씁니다. 한쪽 입장만 있으면 그 안에서 나온 서로 다른 이유를 씁니다.
               - 댓글로 오간 반박이 있으면 어떤 논점이 맞섰는지 씁니다.
               - [집계] 에서 사전→사후 인원이 바뀐 선택지가 있으면 "찬성 3명 → 4명" 처럼 집계 숫자 그대로 한 번 씁니다. 누가 바꿨는지, 모두 바꿨는지는 추정하지 않습니다.
            7. 모든 문장을 "~했어요", "~였어요" 처럼 "~요" 로 끝냅니다. "~습니다", "~다" 로 끝내지 않습니다. insight_text 에 source_id(p:1, c:2 같은 표시)를 쓰지 않습니다.
            8. excluded_ids 를 빼고 나면 요약할 글이 하나도 없으면 다른 내용을 만들지 않고 {"analysis_status": "INSUFFICIENT"} 만 출력합니다. 의미 없는 글로 요약을 지어내지 않습니다.
            출력은 아래 형식의 JSON 객체 하나만 씁니다. 다른 텍스트를 붙이지 않습니다.
            {
              "excluded_ids": ["p:3"],
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
        for (ClassMidVoteInfo choice : input.midChoices()) {
            sb.append("중간투표(콘텐츠 중 선택, 지점 ").append(choice.checkpointId()).append("): ").append(choice.label()).append('\n');
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
                    .append(" | 종류=").append(kindLabel(statement.sourceType()))
                    .append(" | 입장(참고용, 인용 금지)=").append(statement.stance() == null ? "-" : statement.stance()).append('\n');
            if (statement.parentContext() != null && MemberFeedbackInput.SOURCE_DEBATE.equals(statement.sourceType())) {
                sb.append("  (직전 AI 철학자 주장, 맥락 전용·인용 금지: \"").append(statement.parentContext()).append("\")\n");
            } else if (statement.parentContext() != null) {
                sb.append("  (부모 맥락, 타인의 글: \"").append(statement.parentContext()).append("\")\n");
            }
            sb.append("  원문: ").append(statement.content()).append('\n');
        }
        return sb.toString();
    }

    private static String kindLabel(String sourceType) {
        if (MemberFeedbackInput.SOURCE_PERSPECTIVE.equals(sourceType)) {
            return "의견";
        }
        return MemberFeedbackInput.SOURCE_DEBATE.equals(sourceType) ? "토론 반박" : "댓글";
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
            sb.append("사후 ").append(option.postVoteCount()).append("명 (")
                    .append(option.postVoteRatio()).append("%)\n");
        }
        for (ClassResultStats.CheckpointStat checkpoint : stats.midVoteCheckpoints()) {
            sb.append("- 콘텐츠 중 선택 (지점 ").append(checkpoint.checkpointId()).append(", ").append(checkpoint.voteCount()).append("명): ");
            List<String> parts = new ArrayList<>();
            for (ClassResultStats.ChoiceStat choice : checkpoint.options()) {
                parts.add(choice.label() + " " + choice.voteCount() + "명 (" + choice.ratio() + "%)");
            }
            sb.append(String.join(", ", parts)).append('\n');
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

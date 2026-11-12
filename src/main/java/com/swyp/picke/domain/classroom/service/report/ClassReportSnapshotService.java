package com.swyp.picke.domain.classroom.service.report;

import com.swyp.picke.domain.battle.entity.Battle;
import com.swyp.picke.domain.battle.entity.BattleOption;
import com.swyp.picke.domain.battle.repository.BattleOptionRepository;
import com.swyp.picke.domain.classroom.entity.AiJob;
import com.swyp.picke.domain.classroom.entity.ClassDebateMessage;
import com.swyp.picke.domain.classroom.entity.ClassDebateSession;
import com.swyp.picke.domain.classroom.entity.ClassMember;
import com.swyp.picke.domain.classroom.entity.ClassReport;
import com.swyp.picke.domain.classroom.entity.Classroom;
import com.swyp.picke.domain.classroom.enums.AiJobType;
import com.swyp.picke.domain.classroom.enums.ClassDebateSpeaker;
import com.swyp.picke.domain.classroom.enums.ClassReportStatus;
import com.swyp.picke.domain.classroom.enums.ClassReportType;
import com.swyp.picke.domain.classroom.repository.AiJobRepository;
import com.swyp.picke.domain.classroom.repository.ClassDebateMessageRepository;
import com.swyp.picke.domain.classroom.repository.ClassDebateSessionRepository;
import com.swyp.picke.domain.classroom.repository.ClassReportRepository;
import com.swyp.picke.domain.classroom.repository.ClassroomRepository;
import com.swyp.picke.domain.classroom.service.ClassMidVoteService;
import com.swyp.picke.domain.classroom.dto.response.ClassMidVoteInfo;
import com.swyp.picke.domain.perspective.entity.Perspective;
import com.swyp.picke.domain.perspective.entity.PerspectiveComment;
import com.swyp.picke.domain.perspective.enums.PerspectiveStatus;
import com.swyp.picke.domain.perspective.repository.PerspectiveCommentRepository;
import com.swyp.picke.domain.perspective.repository.PerspectiveRepository;
import com.swyp.picke.domain.vote.entity.BattleVote;
import com.swyp.picke.domain.vote.repository.BattleVoteRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 리포트 생성의 DB 단계. AI 호출(수십 초)은 트랜잭션 밖에서 하므로 준비/완료/실패를 각각 짧은 트랜잭션으로 나눈다.
 * <ul>
 *   <li>createSnapshots: 마감 감지 시 1회. CLASS_SUMMARY(통계 스냅샷) + 참여 완료자별 MEMBER_FEEDBACK(PENDING)</li>
 *   <li>prepare: 입력 생성. 글이 없으면 AI 없이 템플릿(LIMITED)으로 끝내고, 아니면 ai_jobs 를 만들고 GENERATING</li>
 *   <li>complete / fail: 결과 반영. 실패는 자동 2회까지 재시도(PENDING 복귀), 그 뒤 FAILED.
 *       개인 리포트가 근거 누락(EVIDENCE_MISSING)으로 끝나면 FAILED 대신 서버 템플릿(LIMITED)</li>
 * </ul>
 * 입력 분량 상한(PRD §12·§14 토큰)은 글자 수로 근사한다 (한글 1자 ≈ 1토큰 이상이므로 보수적).
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ClassReportSnapshotService {

    /** 자동 재시도 포함 최대 시도 횟수. 그 뒤에는 운영자 수동 재시도 1회(MAX_MANUAL_ATTEMPTS) */
    public static final int MAX_AUTO_ATTEMPTS = 2;
    public static final int MAX_MANUAL_ATTEMPTS = 3;
    /** GENERATING 인 채로 이 시간이 지나면 중단된 것으로 보고 되돌린다 (제공자 한도 45초 + 여유) */
    public static final int STALE_GENERATING_MINUTES = 5;

    static final int MEMBER_INPUT_MAX_CHARS = 8_000;
    static final int MEMBER_COMMENT_MAX_COUNT = 20;
    static final int INSIGHT_INPUT_MAX_CHARS = 12_000;
    static final int INSIGHT_POST_MAX_COUNT = 60;
    static final int INSIGHT_POSTS_PER_AUTHOR = 2;
    static final int INSIGHT_MIN_AUTHORS = 3;
    static final int PARENT_CONTEXT_MAX_CHARS = 200;

    public static final String KEY_CUTOFF_AT = "cutoff_at";
    public static final String KEY_STATS = "stats";
    public static final String KEY_INSIGHT = "insight";

    private final ClassroomRepository classroomRepository;
    private final ClassReportRepository classReportRepository;
    private final AiJobRepository aiJobRepository;
    private final ClassResultStatsCalculator statsCalculator;
    private final BattleOptionRepository battleOptionRepository;
    private final BattleVoteRepository battleVoteRepository;
    private final PerspectiveRepository perspectiveRepository;
    private final PerspectiveCommentRepository perspectiveCommentRepository;
    private final ClassReportAiClient aiClient;
    private final ClassMidVoteService classMidVoteService;
    private final ClassDebateSessionRepository debateSessionRepository;
    private final ClassDebateMessageRepository debateMessageRepository;

    /** prepare 결과. needsAi=false 면 이미 템플릿으로 끝난 것 */
    public record Prepared(Long reportId, Long jobId, ClassReportType type, boolean needsAi,
                           MemberFeedbackInput memberInput, ClassInsightInput insightInput) {
    }

    // ===== 1. 마감 스냅샷 =====

    /** 마감된 클래스의 스냅샷 생성. 이미 있으면 0. 반환값은 만든 리포트 수 */
    public int createSnapshots(Long classroomId, LocalDateTime now) {
        Classroom classroom = classroomRepository.findById(classroomId).orElse(null);
        if (classroom == null || classReportRepository.existsByClassroomIdAndType(classroomId, ClassReportType.CLASS_SUMMARY)) {
            return 0;
        }
        ClassResultStatsCalculator.Result result = statsCalculator.calculate(classroom);
        LocalDateTime cutoff = classroom.getEffectiveCloseAt();
        if (cutoff == null || cutoff.isAfter(now)) {
            cutoff = now;
        }
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put(KEY_CUTOFF_AT, cutoff.toString());
        snapshot.put(KEY_STATS, result.stats().toMap());

        List<ClassReport> reports = new ArrayList<>();
        ClassReport summary = ClassReport.builder().classroom(classroom).type(ClassReportType.CLASS_SUMMARY).build();
        summary.attachSnapshot(snapshot);
        reports.add(summary);
        for (ClassMember member : result.completedMembers()) {
            reports.add(ClassReport.builder().classroom(classroom).member(member).type(ClassReportType.MEMBER_FEEDBACK).build());
        }
        classReportRepository.saveAll(reports);
        log.info("[ClassReport] snapshot created classId={} members={} cutoff={}", classroomId, result.completedMembers().size(), cutoff);
        return reports.size();
    }

    // ===== 2. 생성 준비 =====

    public Prepared prepare(Long reportId, LocalDateTime now) {
        ClassReport report = classReportRepository.findById(reportId).orElse(null);
        if (report == null || report.getStatus() != ClassReportStatus.PENDING) {
            return null;
        }
        Classroom classroom = report.getClassroom();
        if (report.getType() == ClassReportType.MEMBER_FEEDBACK) {
            MemberFeedbackInput input = buildMemberInput(classroom, report.getMember());
            if (input.statements().isEmpty()) {
                report.limit(ClassReportContentValidator.limitedMemberFeedback(input.preOption(), input.midChoices(), input.postOption()), now);
                return new Prepared(reportId, null, report.getType(), false, input, null);
            }
            AiJob job = startJob(report, AiJobType.MEMBER_REPORT, now);
            return new Prepared(reportId, job.getId(), report.getType(), true, input, null);
        }
        ClassInsightInput input = buildInsightInput(classroom, report.getContent());
        if (input.distinctAuthorCount() < INSIGHT_MIN_AUTHORS) {
            report.limit(withInsight(report.getContent(), ClassReportContentValidator.limitedClassInsight()), now);
            return new Prepared(reportId, null, report.getType(), false, null, input);
        }
        AiJob job = startJob(report, AiJobType.CLASS_INSIGHT, now);
        return new Prepared(reportId, job.getId(), report.getType(), true, null, input);
    }

    private AiJob startJob(ClassReport report, AiJobType type, LocalDateTime now) {
        Classroom classroom = report.getClassroom();
        ClassMember member = report.getMember();
        String key = type.name() + ":" + classroom.getId() + ":" + (member == null ? "class" : member.getId());
        AiJob job = aiJobRepository.findByIdempotencyKey(key).orElseGet(() -> aiJobRepository.save(AiJob.builder()
                .type(type)
                .idempotencyKey(key)
                .ticket(classroom.getTicket())
                .classroom(classroom)
                .member(member)
                .build()));
        job.start(aiClient.getModel(), now);
        report.startGenerating(job);
        return job;
    }

    // ===== 3. 완료 / 실패 =====

    public void complete(Long reportId, Long jobId, Map<String, Object> validated, ClassReportAiClient.AiChatResult result, LocalDateTime now) {
        ClassReport report = classReportRepository.findById(reportId).orElseThrow();
        AiJob job = aiJobRepository.findById(jobId).orElseThrow();
        job.succeed(result.inputTokens(), result.outputTokens(), now);
        if (report.getType() == ClassReportType.MEMBER_FEEDBACK && validated.get("limited_reason") != null) {
            report.limit(validated, now); // AI 가 분석할 이유가 없다고 판단해 서버 템플릿으로 대체한 경우
        } else if (report.getType() == ClassReportType.MEMBER_FEEDBACK) {
            report.complete(validated, now);
        } else {
            report.complete(withInsight(report.getContent(), validated), now);
        }
    }

    /** rejected=true 는 검증 실패(응답은 왔지만 규칙 위반), false 는 제공자 장애 */
    public void fail(Long reportId, Long jobId, String errorCode, boolean rejected, LocalDateTime now) {
        fail(reportId, jobId, errorCode, rejected, null, now);
    }

    /** fallbackContent 가 있으면 자동 재시도를 다 쓴 뒤 FAILED 대신 그 템플릿으로 LIMITED 게시한다 */
    public void fail(Long reportId, Long jobId, String errorCode, boolean rejected, Map<String, Object> fallbackContent,
                     LocalDateTime now) {
        ClassReport report = classReportRepository.findById(reportId).orElseThrow();
        AiJob job = aiJobRepository.findById(jobId).orElseThrow();
        if (rejected) {
            job.reject(errorCode, now);
        } else {
            job.fail(errorCode, now);
        }
        log.warn("[ClassReport] generation failed reportId={} type={} attempt={} code={} rejected={}",
                reportId, report.getType(), job.getAttempt(), errorCode, rejected);
        if (job.getAttempt() < MAX_AUTO_ATTEMPTS) {
            report.requeue();
        } else if (fallbackContent != null) {
            report.limit(fallbackContent, now);
            log.warn("[ClassReport] fallback to template reportId={} type={} code={}", reportId, report.getType(), errorCode);
        } else {
            report.fail();
        }
    }

    /** 서버 중단 등으로 GENERATING 에 멈춘 리포트 복구 */
    public int recoverStale(LocalDateTime now) {
        int recovered = 0;
        for (ClassReport report : classReportRepository.findByStatusOrderByIdAsc(ClassReportStatus.GENERATING, PageRequest.of(0, 100))) {
            AiJob job = report.getAiJob();
            if (job == null || job.getStartedAt() == null || job.getStartedAt().plusMinutes(STALE_GENERATING_MINUTES).isAfter(now)) {
                continue;
            }
            job.fail("STALE", now);
            if (job.getAttempt() < MAX_AUTO_ATTEMPTS) {
                report.requeue();
            } else {
                report.fail();
            }
            recovered++;
        }
        return recovered;
    }

    // ===== 입력 생성 =====

    /**
     * 개인 리포트 입력: 본인 PUBLISHED 의견 전부 + 커스텀 배틀 토론 반박 전부(최대 3건) + 본인 댓글 최신 20건,
     * 합쳐서 8,000자 안에서 오래된 댓글부터 제외. 같은 내용을 반복한 발언은 처음 것만 남긴다 (엣지 케이스 PRD RP-10·EX-05 중복 논거 집계 금지)
     */
    public MemberFeedbackInput buildMemberInput(Classroom classroom, ClassMember member) {
        Battle battle = classroom.getBattle();
        Long classId = classroom.getId();
        Long userId = member.getUser().getId();
        Map<Long, MemberFeedbackInput.OptionInfo> optionById = new LinkedHashMap<>();
        for (BattleOption option : sortedOptions(battle)) {
            optionById.put(option.getId(), new MemberFeedbackInput.OptionInfo(option.getId(), option.getTitle(), option.getStance()));
        }
        MemberFeedbackInput.OptionInfo pre = null;
        MemberFeedbackInput.OptionInfo post = null;
        BattleVote vote = battleVoteRepository.findByBattleAndUserAndClassroom(battle, member.getUser(), classroom).orElse(null);
        if (vote != null) {
            pre = vote.getPreVoteOption() == null ? null : optionById.get(vote.getPreVoteOption().getId());
            post = vote.getPostVoteOption() == null ? null : optionById.get(vote.getPostVoteOption().getId());
        }

        // 중간투표는 중간투표가 있는 기존 콘텐츠만 분석 소스로 쓴다 (PRD §12 "사전·checkpoint별 중간·사후")
        List<ClassMidVoteInfo> mid = classMidVoteService.choicesOf(classroom, vote);

        List<MemberFeedbackInput.Statement> perspectives = new ArrayList<>();
        for (Perspective p : perspectiveRepository.findByClassroomIdAndUserIdAndStatusOrderByCreatedAtAsc(classId, userId, PerspectiveStatus.PUBLISHED)) {
            perspectives.add(new MemberFeedbackInput.Statement(
                    MemberFeedbackInput.sourceId(MemberFeedbackInput.SOURCE_PERSPECTIVE, p.getId()),
                    MemberFeedbackInput.SOURCE_PERSPECTIVE, p.getId(), p.getCreatedAt(),
                    p.getOption() == null ? null : p.getOption().getStance(), ReportProfanityMasker.mask(p.getContent()), null));
        }
        Set<String> seen = new HashSet<>();
        for (MemberFeedbackInput.Statement p : perspectives) {
            seen.add(dedupKey(p.content()));
        }
        List<MemberFeedbackInput.Statement> debateReplies = debateReplies(classId, member, seen);
        List<MemberFeedbackInput.Statement> comments = new ArrayList<>();
        for (PerspectiveComment c : perspectiveCommentRepository.findByClassroomIdAndUserIdAndHiddenFalseOrderByCreatedAtAsc(classId, userId)) {
            String content = ReportProfanityMasker.mask(c.getContent());
            if (!seen.add(dedupKey(content))) {
                continue;
            }
            Perspective parent = c.getPerspective();
            boolean parentIsMine = parent != null && Objects.equals(parent.getUser().getId(), userId);
            String parentContext = parent == null || parentIsMine ? null
                    : truncate(ReportProfanityMasker.mask(parent.getContent()), PARENT_CONTEXT_MAX_CHARS);
            comments.add(new MemberFeedbackInput.Statement(
                    MemberFeedbackInput.sourceId(MemberFeedbackInput.SOURCE_COMMENT, c.getId()),
                    MemberFeedbackInput.SOURCE_COMMENT, c.getId(), c.getCreatedAt(),
                    parent == null || parent.getOption() == null ? null : parent.getOption().getStance(),
                    content, parentContext));
        }
        int excluded = 0;
        while (comments.size() > MEMBER_COMMENT_MAX_COUNT) {
            comments.remove(0);
            excluded++;
        }
        int chars = chars(perspectives) + chars(debateReplies) + chars(comments);
        while (chars > MEMBER_INPUT_MAX_CHARS && !comments.isEmpty()) {
            chars -= chars(List.of(comments.remove(0)));
            excluded++;
        }
        List<MemberFeedbackInput.Statement> statements = new ArrayList<>(perspectives);
        statements.addAll(debateReplies);
        statements.addAll(comments);
        statements.sort(Comparator.comparing(MemberFeedbackInput.Statement::createdAt, Comparator.nullsLast(Comparator.naturalOrder())));
        return new MemberFeedbackInput(battle.getTitle(), battle.getSummary(), new ArrayList<>(optionById.values()),
                pre, mid, post, statements, excluded);
    }

    /**
     * 커스텀 배틀 토론 반박 (엣지 케이스 PRD EX-02 "배틀 대화와 댓글 분석 구분"). 직전 AI 철학자 주장은 맥락으로만 붙인다
     * (3.4 "AI 유도 질문과 자발적 의견 구분", "AI 응답을 사용자 의견으로 취급하지 않음").
     * 입력 분류에서 분석 불가로 저장된 반박은 뺀다(4.1). 분류가 없는 반박은 넣고 리포트 AI 가 판단한다.
     * 토론은 1:1 대화라 공개 의견 요약(인사이트)에는 넣지 않는다.
     */
    private List<MemberFeedbackInput.Statement> debateReplies(Long classId, ClassMember member, Set<String> seen) {
        List<MemberFeedbackInput.Statement> replies = new ArrayList<>();
        ClassDebateSession session = debateSessionRepository.findByClassroomIdAndMemberId(classId, member.getId()).orElse(null);
        if (session == null) {
            return replies;
        }
        String stance = session.getParticipantOption() == null ? null : session.getParticipantOption().getStance();
        String lastArgument = null;
        for (ClassDebateMessage m : debateMessageRepository.findBySessionIdOrderByRoundIndexAscIdAsc(session.getId())) {
            if (m.getSpeaker() == ClassDebateSpeaker.AI) {
                lastArgument = m.getContent();
                continue;
            }
            String content = ReportProfanityMasker.mask(m.getContent());
            if (Boolean.FALSE.equals(m.getValidForAnalysis()) || !seen.add(dedupKey(content))) {
                continue;
            }
            replies.add(new MemberFeedbackInput.Statement(
                    MemberFeedbackInput.sourceId(MemberFeedbackInput.SOURCE_DEBATE, m.getId()),
                    MemberFeedbackInput.SOURCE_DEBATE, m.getId(), m.getCreatedAt(), stance, content,
                    lastArgument == null ? null : truncate(ReportProfanityMasker.mask(lastArgument), PARENT_CONTEXT_MAX_CHARS)));
        }
        return replies;
    }

    /**
     * 인사이트 입력: 공개 의견·댓글을 작성자별 최신 2건으로 추리고 입장을 번갈아 섞어 최대 60건, 12,000자.
     * 작성자는 익명 번호로만 표기한다 (PRD §14). 한 작성자가 같은 내용을 반복한 글은 처음 것만 남긴다 (RP-10).
     */
    public ClassInsightInput buildInsightInput(Classroom classroom, Map<String, Object> snapshot) {
        Battle battle = classroom.getBattle();
        Long classId = classroom.getId();
        List<MemberFeedbackInput.OptionInfo> options = new ArrayList<>();
        for (BattleOption option : sortedOptions(battle)) {
            options.add(new MemberFeedbackInput.OptionInfo(option.getId(), option.getTitle(), option.getStance()));
        }

        record Raw(Long userId, LocalDateTime createdAt, ClassInsightInput.PublicPost post) {
        }
        List<Raw> raws = new ArrayList<>();
        for (Perspective p : perspectiveRepository.findByClassroomIdAndStatusOrderByCreatedAtAsc(classId, PerspectiveStatus.PUBLISHED)) {
            raws.add(new Raw(p.getUser().getId(), p.getCreatedAt(), new ClassInsightInput.PublicPost(
                    MemberFeedbackInput.sourceId(MemberFeedbackInput.SOURCE_PERSPECTIVE, p.getId()),
                    MemberFeedbackInput.SOURCE_PERSPECTIVE, p.getId(), 0,
                    p.getOption() == null ? null : p.getOption().getId(),
                    p.getOption() == null ? null : p.getOption().getStance(), ReportProfanityMasker.mask(p.getContent()), null)));
        }
        for (PerspectiveComment c : perspectiveCommentRepository.findByClassroomIdAndHiddenFalseOrderByCreatedAtAsc(classId)) {
            Perspective parent = c.getPerspective();
            if (parent == null || parent.getStatus() != PerspectiveStatus.PUBLISHED) {
                continue;
            }
            raws.add(new Raw(c.getUser().getId(), c.getCreatedAt(), new ClassInsightInput.PublicPost(
                    MemberFeedbackInput.sourceId(MemberFeedbackInput.SOURCE_COMMENT, c.getId()),
                    MemberFeedbackInput.SOURCE_COMMENT, c.getId(), 0,
                    parent.getOption() == null ? null : parent.getOption().getId(),
                    parent.getOption() == null ? null : parent.getOption().getStance(), ReportProfanityMasker.mask(c.getContent()),
                    MemberFeedbackInput.sourceId(MemberFeedbackInput.SOURCE_PERSPECTIVE, parent.getId()))));
        }

        // 작성자 익명 번호 (첫 등장 순) + 작성자별 최신 2건
        Map<Long, Integer> authorNo = new LinkedHashMap<>();
        Map<Long, List<Raw>> byAuthor = new LinkedHashMap<>();
        Set<String> seenByAuthor = new HashSet<>();
        for (Raw raw : raws) {
            if (!seenByAuthor.add(raw.userId() + ":" + dedupKey(raw.post().content()))) {
                continue;
            }
            authorNo.computeIfAbsent(raw.userId(), id -> authorNo.size() + 1);
            byAuthor.computeIfAbsent(raw.userId(), id -> new ArrayList<>()).add(raw);
        }
        Comparator<Raw> latestFirst = Comparator.comparing(Raw::createdAt, Comparator.nullsLast(Comparator.reverseOrder()));
        Map<String, List<ClassInsightInput.PublicPost>> byStance = new LinkedHashMap<>();
        for (Map.Entry<Long, List<Raw>> entry : byAuthor.entrySet()) {
            List<Raw> mine = new ArrayList<>(entry.getValue());
            mine.sort(latestFirst);
            int no = authorNo.get(entry.getKey());
            for (Raw raw : mine.subList(0, Math.min(INSIGHT_POSTS_PER_AUTHOR, mine.size()))) {
                ClassInsightInput.PublicPost p = raw.post();
                ClassInsightInput.PublicPost numbered = new ClassInsightInput.PublicPost(p.sourceId(), p.sourceType(), p.id(), no,
                        p.optionId(), p.stance(), p.content(), p.parentSourceId());
                byStance.computeIfAbsent(p.stance() == null ? "" : p.stance(), s -> new ArrayList<>()).add(numbered);
            }
        }
        // 입장을 번갈아 가며 섞는다 (한쪽만 앞에 몰리지 않게)
        List<ClassInsightInput.PublicPost> selected = new ArrayList<>();
        List<List<ClassInsightInput.PublicPost>> queues = new ArrayList<>(byStance.values());
        int chars = 0;
        boolean remaining = true;
        while (remaining && selected.size() < INSIGHT_POST_MAX_COUNT) {
            remaining = false;
            for (List<ClassInsightInput.PublicPost> queue : queues) {
                if (queue.isEmpty()) {
                    continue;
                }
                ClassInsightInput.PublicPost next = queue.get(0);
                int len = next.content() == null ? 0 : next.content().length();
                if (chars + len > INSIGHT_INPUT_MAX_CHARS || selected.size() >= INSIGHT_POST_MAX_COUNT) {
                    queue.clear();
                    continue;
                }
                selected.add(queue.remove(0));
                chars += len;
                remaining = true;
            }
        }
        ClassResultStats stats = snapshot == null ? null : ClassResultStats.fromMap(ReportContent.map(snapshot, KEY_STATS));
        if (stats == null) {
            stats = statsCalculator.calculate(classroom).stats();
        }
        return new ClassInsightInput(battle.getTitle(), battle.getSummary(), options, stats, selected,
                authorNo.size(), raws.size() - selected.size());
    }

    // ===== 헬퍼 =====

    private List<BattleOption> sortedOptions(Battle battle) {
        List<BattleOption> options = new ArrayList<>(battleOptionRepository.findByBattle(battle));
        options.sort(Comparator.comparing(BattleOption::getDisplayOrder, Comparator.nullsLast(Comparator.naturalOrder())));
        return options;
    }

    private static Map<String, Object> withInsight(Map<String, Object> snapshot, Map<String, Object> insight) {
        Map<String, Object> content = snapshot == null ? new LinkedHashMap<>() : new LinkedHashMap<>(snapshot);
        content.put(KEY_INSIGHT, insight);
        return content;
    }

    /** 중복 판정용. 공백·대소문자 차이는 같은 글로 본다 */
    static String dedupKey(String content) {
        return content == null ? "" : content.replaceAll("\\s+", "").toLowerCase(Locale.ROOT);
    }

    private static int chars(List<MemberFeedbackInput.Statement> statements) {
        int total = 0;
        for (MemberFeedbackInput.Statement s : statements) {
            total += s.content() == null ? 0 : s.content().length();
            total += s.parentContext() == null ? 0 : s.parentContext().length();
        }
        return total;
    }

    static String truncate(String text, int max) {
        if (text == null || text.length() <= max) {
            return text;
        }
        return text.substring(0, max) + "…";
    }
}

package com.swyp.picke.domain.classroom.service;

import com.swyp.picke.domain.battle.entity.BattleOption;
import com.swyp.picke.domain.classroom.dto.response.ClassInsightResponse;
import com.swyp.picke.domain.classroom.dto.response.ClassMyReportResponse;
import com.swyp.picke.domain.classroom.dto.response.ClassResultResponse;
import com.swyp.picke.domain.classroom.entity.ClassMember;
import com.swyp.picke.domain.classroom.entity.ClassReport;
import com.swyp.picke.domain.classroom.entity.Classroom;
import com.swyp.picke.domain.classroom.enums.ClassBattleType;
import com.swyp.picke.domain.classroom.enums.ClassMemberRole;
import com.swyp.picke.domain.classroom.enums.ClassReportStatus;
import com.swyp.picke.domain.classroom.enums.ClassReportType;
import com.swyp.picke.domain.classroom.enums.ClassReportViewState;
import com.swyp.picke.domain.classroom.repository.ClassReportRepository;
import com.swyp.picke.domain.classroom.service.ClassAccessService.ClassAccess;
import com.swyp.picke.domain.classroom.service.report.ClassReportSnapshotService;
import com.swyp.picke.domain.classroom.service.report.ClassResultStats;
import com.swyp.picke.domain.classroom.service.report.ClassResultStatsCalculator;
import com.swyp.picke.domain.classroom.service.report.MemberFeedbackInput;
import com.swyp.picke.domain.classroom.service.report.ReportContent;
import com.swyp.picke.domain.perspective.entity.Perspective;
import com.swyp.picke.domain.perspective.entity.PerspectiveComment;
import com.swyp.picke.domain.perspective.enums.PerspectiveStatus;
import com.swyp.picke.domain.perspective.repository.PerspectiveCommentRepository;
import com.swyp.picke.domain.perspective.repository.PerspectiveRepository;
import com.swyp.picke.domain.vote.entity.BattleVote;
import com.swyp.picke.domain.vote.repository.BattleVoteRepository;
import com.swyp.picke.global.common.exception.CustomException;
import com.swyp.picke.global.common.exception.ErrorCode;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 결과 화면 조회. 저장된 리포트 content(JSON)를 응답으로 바꾸고, 근거 글은 현재 상태로 다시 읽어 삭제·숨김된 글은 뺀다.
 * 상태 판정 순서: 마감 전 → BEFORE_CLOSE, 스냅샷 없음(스케줄러 전) → PENDING, 리포트 행 없음 → NOT_ELIGIBLE, 그 외 리포트 status.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClassResultServiceImpl implements ClassResultService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    static final String INSIGHT_FAILED_TEXT = "AI 요약을 준비하지 못했어요";
    static final int HIGHLIGHT_MAX = 2;

    private final ClassAccessService classAccessService;
    private final ClassReportRepository classReportRepository;
    private final ClassResultStatsCalculator statsCalculator;
    private final ClassMidVoteService classMidVoteService;
    private final BattleVoteRepository battleVoteRepository;
    private final PerspectiveRepository perspectiveRepository;
    private final PerspectiveCommentRepository perspectiveCommentRepository;

    @Override
    public ClassResultResponse getResult(Long userId, Long classId) {
        LocalDateTime now = LocalDateTime.now(KST);
        ClassAccess access = classAccessService.requireResultReadable(classId, userId, now);
        Classroom classroom = access.classroom();
        boolean closed = classroom.isClosed(now);
        ClassReport summary = findSummary(classId);

        ClassResultStats stats;
        String basis;
        LocalDateTime cutoffAt = null;
        if (summary != null && summary.getContent() != null && summary.getContent().get(ClassReportSnapshotService.KEY_STATS) != null) {
            stats = ClassResultStats.fromMap(ReportContent.map(summary.getContent(), ClassReportSnapshotService.KEY_STATS));
            basis = "SNAPSHOT";
            cutoffAt = parseTime(ReportContent.str(summary.getContent(), ClassReportSnapshotService.KEY_CUTOFF_AT));
        } else {
            stats = statsCalculator.calculate(classroom).stats();
            basis = "LIVE";
        }

        ClassReportViewState myState = access.member() == null ? null : myReportState(classroom, access.member(), summary, closed);
        ClassReportViewState insightState = access.role() == ClassMemberRole.OWNER ? insightState(summary, closed) : null;
        return new ClassResultResponse(
                classId, access.role(), classroom.getEffectiveCloseAt(), closed,
                classroom.getTicket().getReadGraceEndsAt(), basis, cutoffAt, toStats(stats), myState, insightState);
    }

    @Override
    public ClassMyReportResponse getMyReport(Long userId, Long classId) {
        LocalDateTime now = LocalDateTime.now(KST);
        ClassAccess access = classAccessService.requireResultReadable(classId, userId, now);
        ClassMember member = access.member();
        if (member == null) {
            throw new CustomException(ErrorCode.CLASS_MEMBER_ONLY); // 참여하지 않는 운영자는 개인 리포트가 없다
        }
        Classroom classroom = access.classroom();
        boolean closed = classroom.isClosed(now);
        ClassReport summary = findSummary(classId);
        ClassReport report = closed ? classReportRepository
                .findByClassroomIdAndMemberIdAndType(classId, member.getId(), ClassReportType.MEMBER_FEEDBACK).orElse(null) : null;
        ClassReportViewState state = myReportState(closed, summary, report);

        BattleVote vote = battleVoteRepository.findByBattleAndUserAndClassroom(classroom.getBattle(), member.getUser(), classroom).orElse(null);
        int perspectiveCount = perspectiveRepository
                .findByClassroomIdAndUserIdAndStatusOrderByCreatedAtAsc(classId, userId, PerspectiveStatus.PUBLISHED).size();
        int commentCount = perspectiveCommentRepository.findByClassroomIdAndUserIdAndHiddenFalseOrderByCreatedAtAsc(classId, userId).size();

        ClassMyReportResponse.Feedback feedback = null;
        LocalDateTime generatedAt = null;
        if (report != null && report.isDone()) {
            feedback = toFeedback(report.getContent(), userId);
            generatedAt = report.getGeneratedAt();
        }
        return new ClassMyReportResponse(
                classId, state, classroom.getEffectiveCloseAt(),
                toOption(vote == null ? null : vote.getPreVoteOption()),
                classMidVoteService.choicesOf(classroom, vote),
                toOption(vote == null ? null : vote.getPostVoteOption()),
                perspectiveCount, commentCount, feedback, generatedAt);
    }

    @Override
    public ClassInsightResponse getInsight(Long userId, Long classId) {
        LocalDateTime now = LocalDateTime.now(KST);
        ClassAccess access = requireOwner(classId, userId, now);
        ClassReport summary = findSummary(classId);
        return toInsight(classId, summary, access.classroom().isClosed(now));
    }

    @Override
    @Transactional
    public ClassInsightResponse retryInsight(Long userId, Long classId) {
        LocalDateTime now = LocalDateTime.now(KST);
        ClassAccess access = requireOwner(classId, userId, now);
        ClassReport summary = findSummary(classId);
        if (summary == null || !isRetryable(summary)) {
            throw new CustomException(ErrorCode.CLASS_REPORT_NOT_RETRYABLE);
        }
        summary.requeue(); // 스케줄러가 다음 틱에 같은 ai_job 으로 다시 시도한다
        return toInsight(classId, summary, access.classroom().isClosed(now));
    }

    // ===== 상태 판정 =====

    private ClassAccess requireOwner(Long classId, Long userId, LocalDateTime now) {
        ClassAccess access = classAccessService.requireResultReadable(classId, userId, now);
        if (access.role() != ClassMemberRole.OWNER) {
            throw new CustomException(ErrorCode.CLASS_OWNER_ONLY);
        }
        return access;
    }

    private ClassReport findSummary(Long classId) {
        return classReportRepository.findByClassroomIdAndTypeAndMemberIsNull(classId, ClassReportType.CLASS_SUMMARY).orElse(null);
    }

    private ClassReportViewState myReportState(Classroom classroom, ClassMember member, ClassReport summary, boolean closed) {
        ClassReport report = closed ? classReportRepository
                .findByClassroomIdAndMemberIdAndType(classroom.getId(), member.getId(), ClassReportType.MEMBER_FEEDBACK).orElse(null) : null;
        return myReportState(closed, summary, report);
    }

    private static ClassReportViewState myReportState(boolean closed, ClassReport summary, ClassReport report) {
        if (!closed) {
            return ClassReportViewState.BEFORE_CLOSE;
        }
        if (summary == null) {
            return ClassReportViewState.PENDING; // 마감은 됐지만 스냅샷이 아직 안 만들어짐
        }
        if (report == null) {
            return ClassReportViewState.NOT_ELIGIBLE;
        }
        return toViewState(report.getStatus());
    }

    private static ClassReportViewState insightState(ClassReport summary, boolean closed) {
        if (!closed) {
            return ClassReportViewState.BEFORE_CLOSE;
        }
        if (summary == null) {
            return ClassReportViewState.PENDING;
        }
        return toViewState(summary.getStatus());
    }

    private static ClassReportViewState toViewState(ClassReportStatus status) {
        return switch (status) {
            case PENDING -> ClassReportViewState.PENDING;
            case GENERATING -> ClassReportViewState.GENERATING;
            case SUCCEEDED -> ClassReportViewState.SUCCEEDED;
            case LIMITED -> ClassReportViewState.LIMITED;
            case FAILED -> ClassReportViewState.FAILED;
        };
    }

    private static boolean isRetryable(ClassReport summary) {
        if (summary.getStatus() != ClassReportStatus.FAILED) {
            return false;
        }
        return summary.getAiJob() == null || summary.getAiJob().getAttempt() < ClassReportSnapshotService.MAX_MANUAL_ATTEMPTS;
    }

    // ===== 응답 변환 =====

    private ClassInsightResponse toInsight(Long classId, ClassReport summary, boolean closed) {
        ClassReportViewState state = insightState(summary, closed);
        LocalDateTime cutoffAt = summary == null ? null
                : parseTime(ReportContent.str(summary.getContent(), ClassReportSnapshotService.KEY_CUTOFF_AT));
        boolean retryable = summary != null && isRetryable(summary);
        if (summary == null || !summary.isDone()) {
            String text = state == ClassReportViewState.FAILED ? INSIGHT_FAILED_TEXT : null;
            return new ClassInsightResponse(classId, state, retryable, cutoffAt, null, text, null, 0, List.of(), List.of());
        }
        Map<String, Object> insight = ReportContent.map(summary.getContent(), ClassReportSnapshotService.KEY_INSIGHT);
        List<ClassInsightResponse.PublicPost> evidence = new ArrayList<>();
        for (String id : ReportContent.stringList(insight, "evidence_ids")) {
            ClassInsightResponse.PublicPost post = resolvePublic(id, classId);
            if (post != null) {
                evidence.add(post);
            }
        }
        List<ClassInsightResponse.PublicPost> highlights = new ArrayList<>();
        for (String id : ReportContent.stringList(insight, "highlight_ids")) {
            ClassInsightResponse.PublicPost post = resolvePublic(id, classId);
            if (post != null && highlights.size() < HIGHLIGHT_MAX) {
                highlights.add(post);
            }
        }
        backfillHighlights(highlights, classId);
        return new ClassInsightResponse(
                classId, state, retryable, cutoffAt, summary.getGeneratedAt(),
                ReportContent.str(insight, "insight_text"),
                ReportContent.str(insight, "coverage"),
                ReportContent.asInt(insight, "excluded_count"),
                evidence, highlights);
    }

    /** 하이라이트가 2건 미만이면 최신 공개 의견으로 채운다 (AI 호출 없음) */
    private void backfillHighlights(List<ClassInsightResponse.PublicPost> highlights, Long classId) {
        if (highlights.size() >= HIGHLIGHT_MAX) {
            return;
        }
        List<Perspective> recent = perspectiveRepository.findByClassroomIdAndStatusOrderByCreatedAtAsc(classId, PerspectiveStatus.PUBLISHED);
        for (int i = recent.size() - 1; i >= 0 && highlights.size() < HIGHLIGHT_MAX; i--) {
            Perspective p = recent.get(i);
            boolean exists = highlights.stream().anyMatch(h ->
                    MemberFeedbackInput.SOURCE_PERSPECTIVE.equals(h.sourceType()) && Objects.equals(h.id(), p.getId()));
            if (!exists) {
                highlights.add(toPublic(p));
            }
        }
    }

    private ClassInsightResponse.PublicPost resolvePublic(String sourceId, Long classId) {
        if (sourceId == null || sourceId.length() < 3) {
            return null;
        }
        Long id = ReportContent.asLong(sourceId.substring(2));
        if (id == null) {
            return null;
        }
        if (sourceId.startsWith("p:")) {
            Perspective p = perspectiveRepository.findById(id).orElse(null);
            return p == null || p.getStatus() != PerspectiveStatus.PUBLISHED || !inClass(p.getClassroom(), classId) ? null : toPublic(p);
        }
        PerspectiveComment c = perspectiveCommentRepository.findById(id).orElse(null);
        if (c == null || c.isHidden() || !inClass(c.getClassroom(), classId)) {
            return null;
        }
        BattleOption option = c.getPerspective() == null ? null : c.getPerspective().getOption();
        return new ClassInsightResponse.PublicPost(MemberFeedbackInput.SOURCE_COMMENT, c.getId(),
                option == null ? null : option.getId(), option == null ? null : option.getStance(), c.getContent(), c.getCreatedAt());
    }

    private static ClassInsightResponse.PublicPost toPublic(Perspective p) {
        BattleOption option = p.getOption();
        return new ClassInsightResponse.PublicPost(MemberFeedbackInput.SOURCE_PERSPECTIVE, p.getId(),
                option == null ? null : option.getId(), option == null ? null : option.getStance(), p.getContent(), p.getCreatedAt());
    }

    private ClassMyReportResponse.Feedback toFeedback(Map<String, Object> content, Long userId) {
        List<ClassMyReportResponse.Evidence> evidence = new ArrayList<>();
        for (Map<String, Object> ref : ReportContent.mapList(content, "evidence_refs")) {
            String sourceType = ReportContent.str(ref, "source_type");
            Long sourceId = ReportContent.asLong(ref.get("source_id"));
            if (sourceType == null || sourceId == null) {
                continue;
            }
            String text = null;
            LocalDateTime createdAt = null;
            if (MemberFeedbackInput.SOURCE_PERSPECTIVE.equals(sourceType)) {
                Perspective p = perspectiveRepository.findById(sourceId).orElse(null);
                if (p == null || p.getStatus() != PerspectiveStatus.PUBLISHED || !Objects.equals(p.getUser().getId(), userId)) {
                    continue;
                }
                text = p.getContent();
                createdAt = p.getCreatedAt();
            } else {
                PerspectiveComment c = perspectiveCommentRepository.findById(sourceId).orElse(null);
                if (c == null || c.isHidden() || !Objects.equals(c.getUser().getId(), userId)) {
                    continue;
                }
                text = c.getContent();
                createdAt = c.getCreatedAt();
            }
            evidence.add(new ClassMyReportResponse.Evidence(ReportContent.str(ref, "field"), sourceType, sourceId,
                    ReportContent.str(ref, "quote"), text, createdAt));
        }
        return new ClassMyReportResponse.Feedback(
                ReportContent.str(content, "headline"),
                ReportContent.str(content, "judgment_basis"),
                toSection(ReportContent.map(content, "strength")),
                toSection(ReportContent.map(content, "next_focus")),
                ReportContent.str(content, "next_question"),
                evidence,
                ReportContent.str(content, "analysis_status"),
                ReportContent.str(content, "coverage"),
                ReportContent.asInt(content, "excluded_count"));
    }

    private static ClassMyReportResponse.Section toSection(Map<String, Object> section) {
        if (section == null) {
            return null;
        }
        return new ClassMyReportResponse.Section(ReportContent.str(section, "title"), ReportContent.str(section, "body"));
    }

    private static ClassMyReportResponse.Option toOption(BattleOption option) {
        return option == null ? null : new ClassMyReportResponse.Option(option.getId(), option.getTitle(), option.getStance());
    }

    private static ClassResultResponse.Stats toStats(ClassResultStats stats) {
        List<ClassResultResponse.OptionStat> options = new ArrayList<>();
        for (ClassResultStats.OptionStat o : stats.options()) {
            options.add(new ClassResultResponse.OptionStat(o.optionId(), o.title(), o.stance(), o.preVoteCount(),
                    o.postVoteCount(), o.postVoteRatio()));
        }
        List<ClassResultResponse.CheckpointStat> checkpoints = new ArrayList<>();
        for (ClassResultStats.CheckpointStat c : stats.midVoteCheckpoints()) {
            List<ClassResultResponse.ChoiceStat> choices = new ArrayList<>();
            for (ClassResultStats.ChoiceStat choice : c.options()) {
                choices.add(new ClassResultResponse.ChoiceStat(choice.optionId(), choice.label(), choice.voteCount(), choice.ratio()));
            }
            checkpoints.add(new ClassResultResponse.CheckpointStat(c.checkpointId(), c.voteCount(), choices));
        }
        return new ClassResultResponse.Stats(stats.activeMemberCount(), stats.completedCount(), stats.participationRate(),
                stats.commentWriterCount(), stats.commentRate(), stats.postVoteCount(),
                stats.midVoteEnabled(), stats.midVoteCount(), options, checkpoints);
    }

    private static boolean inClass(Classroom classroom, Long classId) {
        return classroom != null && Objects.equals(classroom.getId(), classId);
    }

    private static LocalDateTime parseTime(String value) {
        if (value == null) {
            return null;
        }
        try {
            return LocalDateTime.parse(value);
        } catch (Exception e) {
            return null;
        }
    }
}

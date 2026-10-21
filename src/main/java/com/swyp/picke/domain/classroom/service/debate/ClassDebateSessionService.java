package com.swyp.picke.domain.classroom.service.debate;

import com.swyp.picke.domain.battle.entity.Battle;
import com.swyp.picke.domain.battle.entity.BattleOption;
import com.swyp.picke.domain.classroom.dto.request.ClassDebateReplyRequest;
import com.swyp.picke.domain.classroom.dto.response.ClassDebateResponse;
import com.swyp.picke.domain.classroom.entity.AiJob;
import com.swyp.picke.domain.classroom.entity.ClassDebateMessage;
import com.swyp.picke.domain.classroom.entity.ClassDebateSession;
import com.swyp.picke.domain.classroom.entity.ClassMember;
import com.swyp.picke.domain.classroom.entity.ClassTopicVersion;
import com.swyp.picke.domain.classroom.entity.Classroom;
import com.swyp.picke.domain.classroom.enums.AiJobStatus;
import com.swyp.picke.domain.classroom.enums.AiJobType;
import com.swyp.picke.domain.classroom.enums.ClassBattleType;
import com.swyp.picke.domain.classroom.enums.ClassDebateSpeaker;
import com.swyp.picke.domain.classroom.enums.ClassDebateStatus;
import com.swyp.picke.domain.classroom.repository.AiJobRepository;
import com.swyp.picke.domain.classroom.repository.ClassDebateMessageRepository;
import com.swyp.picke.domain.classroom.repository.ClassDebateSessionRepository;
import com.swyp.picke.domain.classroom.service.ClassAccessService;
import com.swyp.picke.domain.classroom.service.ClassAccessService.ClassAccess;
import com.swyp.picke.domain.classroom.service.topic.AiUsage;
import com.swyp.picke.domain.user.entity.User;
import com.swyp.picke.domain.user.enums.PhilosopherType;
import com.swyp.picke.domain.user.enums.UserBattleStep;
import com.swyp.picke.domain.user.repository.UserRepository;
import com.swyp.picke.domain.user.service.UserBattleService;
import com.swyp.picke.domain.vote.entity.BattleVote;
import com.swyp.picke.domain.vote.repository.BattleVoteRepository;
import com.swyp.picke.global.common.exception.CustomException;
import com.swyp.picke.global.common.exception.ErrorCode;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 토론 세션의 상태 변경을 맡는다. AI 호출은 {@link ClassDebateService} 가 트랜잭션 밖에서 하고,
 * 이 서비스는 (1) 준비(권한·상태 확인, 반박 저장, ai_jobs 시작) (2) 결과 반영 을 각각 짧은 트랜잭션으로 처리한다.
 * <ul>
 *   <li>단계 이동은 저장된 턴 수와 세션 상태만 믿는다. 모델 응답의 라운드·종료 문구는 쓰지 않는다 (PRD §8)</li>
 *   <li>반박·재시도는 세션 행 잠금으로 두 기기의 동시 진행을 막는다</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClassDebateSessionService {

    /** 이 시간보다 오래 GENERATING 이면 멈춘 작업으로 보고 재시도를 허용한다 (호출 45초 × 최대 3회 + 여유) */
    static final long GENERATING_STALE_MINUTES = 3;
    public static final int REPLY_MIN_LENGTH = 50;
    /** PRD 는 반박 길이 제한이 없지만, 호출당 입력 5,000 토큰 예산(§11) 안에 3라운드 이력이 들어가도록 상한을 둔다 */
    public static final int REPLY_MAX_LENGTH = 1_000;
    static final String AI_NOTICE = "철학자의 관점을 응용한 AI의 주장이에요. 실제 인물이 한 말이 아니에요.";

    private final ClassAccessService classAccessService;
    private final ClassDebateSessionRepository sessionRepository;
    private final ClassDebateMessageRepository messageRepository;
    private final BattleVoteRepository battleVoteRepository;
    private final UserRepository userRepository;
    private final UserBattleService userBattleService;
    private final AiJobRepository aiJobRepository;

    /** AI 주장 1건을 만들 준비가 된 상태 */
    public record PreparedTurn(Long sessionId, Long jobId, int round, DebatePromptInput input) {
    }

    // ---------- 시작 ----------

    /**
     * 사전투표한 선택지의 반대편과 그 선택지에 매핑된 철학자로 세션을 만들고 1라운드 AI 주장을 준비한다.
     * 이미 세션이 있으면 null (재접속 = 저장된 대화 반환). 시작하면 사전투표는 잠긴다 (PRD §8 "사전투표를 바꿔 AI 재생성 불가").
     */
    @Transactional
    public PreparedTurn start(Long userId, Long classId, String model, LocalDateTime now) {
        ClassAccess access = classAccessService.requireWritable(classId, userId, now);
        Classroom classroom = requireCustom(access.classroom());
        ClassMember member = access.member();
        if (sessionRepository.findByClassroomIdAndMemberId(classId, member.getId()).isPresent()) {
            return null;
        }
        Battle battle = classroom.getBattle();
        User user = userRepository.findById(userId).orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        BattleOption participant = battleVoteRepository.findByBattleAndUserAndClassroom(battle, user, classroom)
                .map(BattleVote::getPreVoteOption)
                .orElseThrow(() -> new CustomException(ErrorCode.CLASS_DEBATE_PRE_VOTE_REQUIRED));
        BattleOption opponent = battle.getOptions().stream()
                .filter(option -> !option.getId().equals(participant.getId()))
                .findFirst()
                .orElseThrow(() -> new CustomException(ErrorCode.CLASS_BATTLE_NOT_AVAILABLE));

        ClassDebateSession session = sessionRepository.save(ClassDebateSession.builder()
                .classroom(classroom)
                .member(member)
                .participantOption(participant)
                .opponentOption(opponent)
                .philosopher(philosopherOf(classroom.getTopicVersion(), opponent))
                .now(now)
                .build());
        userBattleService.upsertStep(user, battle, classroom, UserBattleStep.TTS_LISTENING);
        AiJob job = startJob(session, 1, model, now);
        return new PreparedTurn(session.getId(), job.getId(), 1, promptInput(session));
    }

    // ---------- 반박 ----------

    /**
     * 반박을 저장하고 다음 라운드 AI 주장을 준비한다. 3라운드 반박이면 토론을 끝내고 사후투표 단계로 넘긴다(null).
     * 같은 키 재전송은 아무것도 바꾸지 않고(null), 다른 키로 같은 라운드 재제출은 409 (PRD §8 재접속과 중복 전송).
     */
    @Transactional
    public PreparedTurn reply(Long userId, Long classId, ClassDebateReplyRequest request, String model, LocalDateTime now) {
        ClassAccess access = classAccessService.requireWritable(classId, userId, now);
        Classroom classroom = requireCustom(access.classroom());
        ClassDebateSession session = sessionRepository.findForUpdate(classId, access.member().getId())
                .orElseThrow(() -> new CustomException(ErrorCode.CLASS_DEBATE_NOT_FOUND));
        int round = request.roundIndex();

        var submitted = messageRepository.findBySessionIdAndRoundIndexAndSpeaker(session.getId(), round, ClassDebateSpeaker.USER);
        if (submitted.isPresent()) {
            if (request.requestKey().equals(submitted.get().getRequestKey())) {
                return null;
            }
            throw new CustomException(ErrorCode.CLASS_DEBATE_ROUND_CONFLICT);
        }
        if (session.getStatus() != ClassDebateStatus.WAITING_USER || session.getRound() != round) {
            throw new CustomException(ErrorCode.CLASS_DEBATE_NOT_WAITING);
        }
        String content = request.content() == null ? "" : request.content().strip();
        int length = content.codePointCount(0, content.length());
        if (length < REPLY_MIN_LENGTH || length > REPLY_MAX_LENGTH) {
            throw new CustomException(ErrorCode.CLASS_DEBATE_REPLY_INVALID);
        }
        messageRepository.save(ClassDebateMessage.builder()
                .session(session)
                .roundIndex(round)
                .speaker(ClassDebateSpeaker.USER)
                .content(content)
                .requestKey(request.requestKey())
                .build());

        if (round == ClassDebateSession.ROUNDS) {
            session.complete(now);
            User user = userRepository.findById(userId).orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
            userBattleService.upsertStep(user, classroom.getBattle(), classroom, UserBattleStep.POST_VOTE);
            return null;
        }
        session.startGenerating(round + 1, now);
        AiJob job = startJob(session, round + 1, model, now);
        return new PreparedTurn(session.getId(), job.getId(), round + 1, promptInput(session));
    }

    // ---------- 재시도 ----------

    /** 실패했거나 오래 멈춘 라운드의 AI 주장을 같은 작업으로 다시 만든다 (부록 D "재시도 가능할 때만 버튼, 같은 job 사용") */
    @Transactional
    public PreparedTurn retry(Long userId, Long classId, String model, LocalDateTime now) {
        ClassAccess access = classAccessService.requireWritable(classId, userId, now);
        requireCustom(access.classroom());
        ClassDebateSession session = sessionRepository.findForUpdate(classId, access.member().getId())
                .orElseThrow(() -> new CustomException(ErrorCode.CLASS_DEBATE_NOT_FOUND));
        if (!session.isRetryable(now.minusMinutes(GENERATING_STALE_MINUTES))) {
            throw new CustomException(ErrorCode.CLASS_DEBATE_NOT_RETRYABLE);
        }
        session.startGenerating(session.getRound(), now);
        AiJob job = startJob(session, session.getRound(), model, now);
        return new PreparedTurn(session.getId(), job.getId(), session.getRound(), promptInput(session));
    }

    // ---------- AI 결과 반영 ----------

    /** 검증을 통과한 주장만 저장하고 반박 대기로 넘긴다. 그 사이 상태가 바뀌었으면(오래된 작업) 버린다 */
    @Transactional
    public void completeTurn(PreparedTurn turn, ClassDebateContentValidator.Argument argument, AiUsage usage, LocalDateTime now) {
        ClassDebateSession session = sessionRepository.findByIdForUpdate(turn.sessionId()).orElseThrow();
        AiJob job = aiJobRepository.findById(turn.jobId()).orElseThrow();
        if (job.getStatus() != AiJobStatus.RUNNING
                || session.getStatus() != ClassDebateStatus.GENERATING || session.getRound() != turn.round()) {
            return;
        }
        if (messageRepository.findBySessionIdAndRoundIndexAndSpeaker(session.getId(), turn.round(), ClassDebateSpeaker.AI).isEmpty()) {
            Long replyTo = turn.round() == 1 ? null
                    : messageRepository.findBySessionIdAndRoundIndexAndSpeaker(session.getId(), turn.round() - 1, ClassDebateSpeaker.USER)
                            .map(ClassDebateMessage::getId).orElse(null);
            messageRepository.save(ClassDebateMessage.builder()
                    .session(session)
                    .roundIndex(turn.round())
                    .speaker(ClassDebateSpeaker.AI)
                    .content(argument.text())
                    .focus(argument.focus())
                    .replyToMessageId(replyTo)
                    .build());
        }
        session.waitForUser();
        addExtraAttempts(job, usage);
        job.succeed(usage.inputTokens(), usage.outputTokens(), now);
    }

    @Transactional
    public void failTurn(PreparedTurn turn, String errorCode, boolean rejected, AiUsage usage, LocalDateTime now) {
        ClassDebateSession session = sessionRepository.findByIdForUpdate(turn.sessionId()).orElseThrow();
        AiJob job = aiJobRepository.findById(turn.jobId()).orElseThrow();
        if (job.getStatus() != AiJobStatus.RUNNING) {
            return;
        }
        if (session.getStatus() == ClassDebateStatus.GENERATING && session.getRound() == turn.round()) {
            session.fail();
        }
        addExtraAttempts(job, usage);
        if (rejected) {
            job.reject(errorCode, now);
        } else {
            job.fail(errorCode, now);
        }
    }

    // ---------- 조회 ----------

    /** 저장된 대화를 그대로 돌려준다. 마감 후에도 읽을 수 있지만 반박·재시도는 막힌다 (PRD §8 입력 및 중단) */
    public ClassDebateResponse view(Long userId, Long classId, LocalDateTime now) {
        ClassAccess access = classAccessService.requireReadable(classId, userId);
        if (access.member() == null) {
            throw new CustomException(ErrorCode.CLASS_DEBATE_NOT_FOUND);
        }
        ClassDebateSession session = sessionRepository.findByClassroomIdAndMemberId(classId, access.member().getId())
                .orElseThrow(() -> new CustomException(ErrorCode.CLASS_DEBATE_NOT_FOUND));
        boolean writable = access.canWrite() && !access.classroom().isClosed(now);
        List<ClassDebateMessage> messages = messageRepository.findBySessionIdOrderByRoundIndexAscIdAsc(session.getId());
        PhilosopherType philosopher = session.getPhilosopher();
        return new ClassDebateResponse(
                session.getId(),
                session.getStatus(),
                session.getRound(),
                writable && session.getStatus() == ClassDebateStatus.WAITING_USER,
                writable && session.isRetryable(now.minusMinutes(GENERATING_STALE_MINUTES)),
                new ClassDebateResponse.Philosopher(philosopher, philosopher.getLabel(), philosopher.getTypeName(), philosopher.getImageKey()),
                side(session.getParticipantOption()),
                side(session.getOpponentOption()),
                messages.stream().map(m -> new ClassDebateResponse.Message(
                        m.getId(), m.getRoundIndex(), m.getSpeaker(), m.getContent(), m.getFocus(),
                        m.getFocus() == null ? null : m.getFocus().getLabel(), m.getReplyToMessageId(), m.getCreatedAt())).toList(),
                AI_NOTICE,
                REPLY_MIN_LENGTH,
                REPLY_MAX_LENGTH);
    }

    /** 커스텀 클래스 사후투표 조건: 토론 완료 (PRD §8 "종료 팝업 → 사후투표") */
    public boolean isCompleted(Long classId, Long memberId) {
        return sessionRepository.existsByClassroomIdAndMemberIdAndStatus(classId, memberId, ClassDebateStatus.COMPLETED);
    }

    // ---------- 내부 ----------

    private static Classroom requireCustom(Classroom classroom) {
        if (classroom.getBattleType() != ClassBattleType.CUSTOM) {
            throw new CustomException(ErrorCode.CLASS_DEBATE_CUSTOM_ONLY);
        }
        return classroom;
    }

    /** 주제 버전에서 선택지별로 매핑한 철학자 (display_order 1 = A). 3라운드 동안 고정 */
    private static PhilosopherType philosopherOf(ClassTopicVersion version, BattleOption opponent) {
        return Integer.valueOf(1).equals(opponent.getDisplayOrder())
                ? version.getOptionAPhilosopher()
                : version.getOptionBPhilosopher();
    }

    private DebatePromptInput promptInput(ClassDebateSession session) {
        Battle battle = session.getClassroom().getBattle();
        List<DebatePromptInput.Turn> history = messageRepository.findBySessionIdOrderByRoundIndexAscIdAsc(session.getId()).stream()
                .map(m -> new DebatePromptInput.Turn(m.getRoundIndex(), m.getSpeaker(), m.getContent()))
                .toList();
        return new DebatePromptInput(
                battle.getTitle(),
                battle.getDescription(),
                promptSide(session.getParticipantOption()),
                promptSide(session.getOpponentOption()),
                session.getPhilosopher(),
                session.getRound(),
                history);
    }

    private static DebatePromptInput.Side promptSide(BattleOption option) {
        return new DebatePromptInput.Side(position(option), option.getTitle(), option.getStance());
    }

    private static ClassDebateResponse.Side side(BattleOption option) {
        return new ClassDebateResponse.Side(option.getId(), position(option), option.getTitle(), option.getStance());
    }

    private static String position(BattleOption option) {
        return Integer.valueOf(1).equals(option.getDisplayOrder()) ? "A" : "B";
    }

    /** 라운드 1건 = 작업 1개. 재시도는 같은 행을 다시 시작해 attempt 를 누적한다 */
    private AiJob startJob(ClassDebateSession session, int round, String model, LocalDateTime now) {
        String key = AiJobType.DEBATE_ARGUMENT.name() + ":" + session.getId() + ":" + round;
        AiJob job = aiJobRepository.findByIdempotencyKey(key)
                .orElseGet(() -> aiJobRepository.save(AiJob.builder()
                        .type(AiJobType.DEBATE_ARGUMENT)
                        .idempotencyKey(key)
                        .ticket(session.getClassroom().getTicket())
                        .classroom(session.getClassroom())
                        .member(session.getMember())
                        .build()));
        job.start(model, now);
        return job;
    }

    private static void addExtraAttempts(AiJob job, AiUsage usage) {
        for (int i = 1; i < usage.calls(); i++) {
            job.addAttempt();
        }
    }
}

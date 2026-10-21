package com.swyp.picke.domain.classroom.service.topic;

import com.swyp.picke.domain.battle.enums.BattleCategory;
import com.swyp.picke.domain.battle.enums.BattleDifficulty;
import com.swyp.picke.domain.classroom.converter.ClassTopicConverter;
import com.swyp.picke.domain.classroom.dto.request.ClassTopicDraftCreateRequest;
import com.swyp.picke.domain.classroom.dto.request.ClassTopicSelectRequest;
import com.swyp.picke.domain.classroom.dto.request.ClassTopicSuggestRequest;
import com.swyp.picke.domain.classroom.dto.request.ClassTopicValidationRequest;
import com.swyp.picke.domain.classroom.dto.response.ClassTopicDraftResponse;
import com.swyp.picke.domain.classroom.dto.response.ClassTopicValidationResponse;
import com.swyp.picke.domain.classroom.dto.response.ClassTopicVersionResponse;
import com.swyp.picke.domain.classroom.entity.AiJob;
import com.swyp.picke.domain.classroom.entity.ClassTopicDraft;
import com.swyp.picke.domain.classroom.entity.ClassTopicVersion;
import com.swyp.picke.domain.classroom.enums.AiJobStatus;
import com.swyp.picke.domain.classroom.enums.AiJobType;
import com.swyp.picke.domain.classroom.enums.ClassTopicVersionSource;
import com.swyp.picke.domain.classroom.repository.AiJobRepository;
import com.swyp.picke.domain.classroom.repository.ClassTopicDraftRepository;
import com.swyp.picke.domain.classroom.repository.ClassTopicVersionRepository;
import com.swyp.picke.domain.ticket.entity.ClassTicket;
import com.swyp.picke.domain.ticket.enums.TicketStatus;
import com.swyp.picke.domain.ticket.repository.ClassTicketRepository;
import com.swyp.picke.domain.user.entity.User;
import com.swyp.picke.domain.user.repository.UserRepository;
import com.swyp.picke.global.common.exception.CustomException;
import com.swyp.picke.global.common.exception.ErrorCode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 주제 초안의 상태 변경을 맡는다. AI 호출은 {@link ClassTopicAiService} 가 트랜잭션 밖에서 하고,
 * 이 서비스는 (1) 준비(권한·쿼터 확인, ai_jobs 생성) (2) 결과 반영 을 각각 짧은 트랜잭션으로 처리한다 (PRD §9 실행 순서).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClassTopicDraftService {

    /** 이 시간보다 오래 GENERATING 이면 멈춘 작업으로 보고 새 요청을 받는다 (호출 45초 × 최대 3회 + 여유) */
    static final long GENERATING_STALE_MINUTES = 3;
    /** 계정별 분당 제안 요청 상한 (PRD §10) */
    static final int SUGGEST_PER_MINUTE = 2;

    private final ClassTopicDraftRepository draftRepository;
    private final ClassTopicVersionRepository versionRepository;
    private final ClassTicketRepository classTicketRepository;
    private final AiJobRepository aiJobRepository;
    private final UserRepository userRepository;
    private final ClassTopicContentValidator validator;

    public record PreparedSuggest(Long draftId, Long jobId, TopicConditions conditions) {
    }

    /** immediate 가 있으면 AI 를 호출하지 않고 그 결과를 그대로 돌려준다 (형식 오류, 바탕 버전과 동일) */
    public record PreparedValidation(Long draftId, Long jobId, Long baseVersionId, TopicContent edit,
                                     ClassTopicValidationResponse immediate) {
    }

    @Transactional
    public ClassTopicDraftResponse create(Long userId, ClassTopicDraftCreateRequest request, LocalDateTime now) {
        User owner = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        ClassTicket ticket = classTicketRepository.findByRegisteredUserIdAndStatus(userId, TicketStatus.ACTIVE)
                .filter(t -> t.isUsable(now))
                .orElseThrow(() -> new CustomException(ErrorCode.CLASS_TICKET_REQUIRED));
        TopicConditions conditions = conditions(request.topicSeed(), request.difficulty(), request.categories());
        ClassTopicDraft draft = draftRepository.save(ClassTopicDraft.builder()
                .owner(owner)
                .ticket(ticket)
                .topicSeed(conditions.topicSeed())
                .difficulty(conditions.difficulty())
                .categories(conditions.categoryNames())
                .build());
        return ClassTopicConverter.toDraft(draft, null);
    }

    public ClassTopicDraftResponse get(Long userId, Long draftId) {
        ClassTopicDraft draft = draftRepository.findById(draftId)
                .filter(d -> d.isOwnedBy(userId))
                .orElseThrow(() -> new CustomException(ErrorCode.CLASS_TOPIC_DRAFT_NOT_FOUND));
        return ClassTopicConverter.toDraft(draft, versionRepository.findTopByDraftIdOrderByVersionNoDesc(draftId).orElse(null));
    }

    // ---------- AI 주제 제안 ----------

    @Transactional
    public PreparedSuggest prepareSuggest(Long userId, Long draftId, ClassTopicSuggestRequest request, String model,
                                          LocalDateTime now) {
        ClassTopicDraft draft = lockOwned(userId, draftId);
        requireUsableTicket(draft, now);
        TopicConditions conditions = request == null ? conditionsOf(draft) : conditions(
                request.topicSeed() != null ? request.topicSeed() : draft.getTopicSeed(),
                request.difficulty() != null ? request.difficulty() : draft.getDifficulty(),
                request.categories() != null ? request.categories() : conditionsOf(draft).categories());

        if (draft.isGenerating(now.minusMinutes(GENERATING_STALE_MINUTES))) {
            throw new CustomException(ErrorCode.CLASS_TOPIC_GENERATING);
        }
        if (draft.remainingSuggestions() == 0) {
            throw new CustomException(ErrorCode.CLASS_TOPIC_SUGGEST_LIMIT);
        }
        long recent = aiJobRepository.countByTicketIdAndTypeAndStartedAtAfter(
                draft.getTicket().getId(), AiJobType.TOPIC_CANDIDATES, now.minusMinutes(1));
        if (recent >= SUGGEST_PER_MINUTE) {
            throw new CustomException(ErrorCode.CLASS_TOPIC_RATE_LIMITED);
        }

        draft.startSuggest(now);
        AiJob job = startJob(AiJobType.TOPIC_CANDIDATES,
                AiJobType.TOPIC_CANDIDATES.name() + ":" + draft.getId() + ":" + draft.getSuggestRequestCount(),
                draft.getTicket(), model, now);
        return new PreparedSuggest(draft.getId(), job.getId(), conditions);
    }

    @Transactional
    public void completeSuggest(PreparedSuggest prepared, List<TopicContent> candidates, AiUsage usage, LocalDateTime now) {
        ClassTopicDraft draft = draftRepository.findByIdForUpdate(prepared.draftId()).orElseThrow();
        AiJob job = aiJobRepository.findById(prepared.jobId()).orElseThrow();
        if (job.getStatus() != AiJobStatus.RUNNING) {
            return;
        }
        List<Map<String, Object>> stored = new ArrayList<>();
        for (int i = 0; i < candidates.size(); i++) {
            stored.add(candidates.get(i).toMap(i + 1));
        }
        TopicConditions conditions = prepared.conditions();
        draft.completeSuggest(conditions.topicSeed(), conditions.difficulty(), conditions.categoryNames(), stored);
        succeedJob(job, usage, now);
    }

    @Transactional
    public void clarifySuggest(PreparedSuggest prepared, String message, AiUsage usage, LocalDateTime now) {
        ClassTopicDraft draft = draftRepository.findByIdForUpdate(prepared.draftId()).orElseThrow();
        AiJob job = aiJobRepository.findById(prepared.jobId()).orElseThrow();
        if (job.getStatus() != AiJobStatus.RUNNING) {
            return;
        }
        draft.needClarification(message);
        succeedJob(job, usage, now);
    }

    /** 실패해도 이전 성공 배치 후보는 그대로 두고 횟수도 차감하지 않는다 (PRD §10) */
    @Transactional
    public void failSuggest(PreparedSuggest prepared, String errorCode, boolean rejected, AiUsage usage, LocalDateTime now) {
        ClassTopicDraft draft = draftRepository.findByIdForUpdate(prepared.draftId()).orElseThrow();
        AiJob job = aiJobRepository.findById(prepared.jobId()).orElseThrow();
        if (job.getStatus() != AiJobStatus.RUNNING) {
            return;
        }
        draft.failSuggest();
        finishJob(job, errorCode, rejected, usage, now);
    }

    // ---------- 후보 선택 ----------

    /** AI 추가 호출 없이 후보를 검증된 주제 버전으로 저장한다. 같은 후보를 다시 고르면 기존 버전을 돌려준다 */
    @Transactional
    public ClassTopicVersionResponse select(Long userId, Long draftId, ClassTopicSelectRequest request) {
        ClassTopicDraft draft = lockOwned(userId, draftId);
        if (draft.getBatchNo() == 0 || request.batchNo() != draft.getBatchNo()) {
            throw new CustomException(ErrorCode.CLASS_TOPIC_CANDIDATE_STALE);
        }
        Map<String, Object> candidate = draft.getCandidates().stream()
                .filter(c -> TopicContent.candidateIdOf(c) == request.candidateId())
                .findFirst()
                .orElseThrow(() -> new CustomException(ErrorCode.CLASS_TOPIC_CANDIDATE_NOT_FOUND));

        String sourceRef = "b" + draft.getBatchNo() + ":c" + request.candidateId();
        ClassTopicVersion version = versionRepository.findFirstByDraftIdAndSourceRefOrderByVersionNoDesc(draftId, sourceRef)
                .orElseGet(() -> saveVersion(draft, ClassTopicVersionSource.CANDIDATE, sourceRef,
                        TopicContent.fromMap(candidate)));
        return ClassTopicConverter.toVersion(version);
    }

    // ---------- 직접 수정 검증 ----------

    @Transactional
    public PreparedValidation prepareValidation(Long userId, Long draftId, ClassTopicValidationRequest request,
                                                String model, LocalDateTime now) {
        ClassTopicDraft draft = lockOwned(userId, draftId);
        requireUsableTicket(draft, now);
        ClassTopicVersion base = versionRepository.findByIdAndDraftId(request.baseVersionId(), draftId)
                .orElseThrow(() -> new CustomException(ErrorCode.CLASS_TOPIC_VERSION_NOT_FOUND));
        TopicContent edit = editOf(base, request);

        List<TopicIssue> formatIssues = validator.formatIssues(edit);
        if (!formatIssues.isEmpty()) {
            return immediate(draft, ClassTopicConverter.toValidation(formatIssues, null, draft.remainingValidations()));
        }
        if (sameContent(base, edit)) {
            return immediate(draft, ClassTopicConverter.toValidation(List.of(), base, draft.remainingValidations()));
        }
        if (draft.remainingValidations() == 0) {
            throw new CustomException(ErrorCode.CLASS_TOPIC_VALIDATION_LIMIT);
        }
        draft.consumeValidation();
        AiJob job = startJob(AiJobType.TOPIC_VALIDATION,
                AiJobType.TOPIC_VALIDATION.name() + ":" + draft.getId() + ":" + draft.getValidationCount(),
                draft.getTicket(), model, now);
        return new PreparedValidation(draft.getId(), job.getId(), base.getId(), edit, null);
    }

    /** issues 가 비어 있으면 새 버전을 만든다. 통과해도 ID 는 저장 성공 뒤 서버가 발급한다 (부록 A) */
    @Transactional
    public ClassTopicValidationResponse completeValidation(PreparedValidation prepared, List<TopicIssue> issues,
                                                           AiUsage usage, LocalDateTime now) {
        ClassTopicDraft draft = draftRepository.findByIdForUpdate(prepared.draftId()).orElseThrow();
        AiJob job = aiJobRepository.findById(prepared.jobId()).orElseThrow();
        succeedJob(job, usage, now);
        ClassTopicVersion version = null;
        if (issues.isEmpty()) {
            ClassTopicVersion base = versionRepository.findById(prepared.baseVersionId()).orElseThrow();
            version = saveVersion(draft, ClassTopicVersionSource.EDITED, "v" + base.getVersionNo(), prepared.edit());
        }
        return ClassTopicConverter.toValidation(issues, version, draft.remainingValidations());
    }

    /** 제공자 장애로 판정을 얻지 못하면 검증 횟수를 돌려준다 */
    @Transactional
    public void failValidation(PreparedValidation prepared, String errorCode, boolean rejected, AiUsage usage,
                               LocalDateTime now) {
        ClassTopicDraft draft = draftRepository.findByIdForUpdate(prepared.draftId()).orElseThrow();
        AiJob job = aiJobRepository.findById(prepared.jobId()).orElseThrow();
        draft.refundValidation();
        finishJob(job, errorCode, rejected, usage, now);
    }

    // ---------- 내부 ----------

    private ClassTopicDraft lockOwned(Long userId, Long draftId) {
        return draftRepository.findByIdForUpdate(draftId)
                .filter(d -> d.isOwnedBy(userId))
                .orElseThrow(() -> new CustomException(ErrorCode.CLASS_TOPIC_DRAFT_NOT_FOUND));
    }

    /** 초안을 만든 이용권이 아직 사용 중이어야 AI 를 호출할 수 있다 (PRD §9 이용권 확인 → 쿼터 확인) */
    private static void requireUsableTicket(ClassTopicDraft draft, LocalDateTime now) {
        ClassTicket ticket = draft.getTicket();
        if (ticket.getStatus() != TicketStatus.ACTIVE || !ticket.isUsable(now)) {
            throw new CustomException(ErrorCode.CLASS_TICKET_REQUIRED);
        }
    }

    private static TopicConditions conditions(String topicSeed, BattleDifficulty difficulty, List<BattleCategory> categories) {
        String seed = ClassTopicContentValidator.trimmed(topicSeed);
        if (seed == null
                || ClassTopicContentValidator.length(seed) < ClassTopicDraft.TOPIC_SEED_MIN_LENGTH
                || ClassTopicContentValidator.length(seed) > ClassTopicDraft.TOPIC_SEED_MAX_LENGTH
                || difficulty == null || categories == null || categories.contains(null)) {
            throw new CustomException(ErrorCode.CLASS_TOPIC_INVALID_CONDITION);
        }
        List<BattleCategory> distinct = List.copyOf(new LinkedHashSet<>(categories));
        if (distinct.isEmpty() || distinct.size() > ClassTopicDraft.MAX_CATEGORIES) {
            throw new CustomException(ErrorCode.CLASS_TOPIC_INVALID_CONDITION);
        }
        return new TopicConditions(seed, difficulty, distinct);
    }

    private static TopicConditions conditionsOf(ClassTopicDraft draft) {
        return new TopicConditions(draft.getTopicSeed(), draft.getDifficulty(),
                draft.getCategories().stream().map(BattleCategory::valueOf).toList());
    }

    /** 수정안은 앞뒤 공백만 정리하고 그대로 보존한다. 철학자를 비우면 바탕 버전 값을 쓴다 */
    private static TopicContent editOf(ClassTopicVersion base, ClassTopicValidationRequest request) {
        ClassTopicValidationRequest.Option a = request.options().get(0);
        ClassTopicValidationRequest.Option b = request.options().get(1);
        return new TopicContent(
                ClassTopicContentValidator.trimmed(request.question()),
                ClassTopicContentValidator.trimmed(request.description()),
                base.getCategory(),
                base.getDifficulty(),
                new TopicContent.Option(ClassTopicContentValidator.trimmed(a.label()), ClassTopicContentValidator.trimmed(a.rationale()),
                        a.philosopher() != null ? a.philosopher() : base.getOptionAPhilosopher()),
                new TopicContent.Option(ClassTopicContentValidator.trimmed(b.label()), ClassTopicContentValidator.trimmed(b.rationale()),
                        b.philosopher() != null ? b.philosopher() : base.getOptionBPhilosopher()));
    }

    private static boolean sameContent(ClassTopicVersion base, TopicContent edit) {
        return Objects.equals(base.getQuestion(), edit.question())
                && Objects.equals(base.getDescription(), edit.description())
                && Objects.equals(base.getOptionALabel(), edit.optionA().label())
                && Objects.equals(base.getOptionARationale(), edit.optionA().rationale())
                && base.getOptionAPhilosopher() == edit.optionA().philosopher()
                && Objects.equals(base.getOptionBLabel(), edit.optionB().label())
                && Objects.equals(base.getOptionBRationale(), edit.optionB().rationale())
                && base.getOptionBPhilosopher() == edit.optionB().philosopher();
    }

    private static PreparedValidation immediate(ClassTopicDraft draft, ClassTopicValidationResponse response) {
        return new PreparedValidation(draft.getId(), null, null, null, response);
    }

    private ClassTopicVersion saveVersion(ClassTopicDraft draft, ClassTopicVersionSource source, String sourceRef,
                                          TopicContent content) {
        int versionNo = versionRepository.findTopByDraftIdOrderByVersionNoDesc(draft.getId())
                .map(v -> v.getVersionNo() + 1)
                .orElse(1);
        return versionRepository.save(ClassTopicVersion.builder()
                .draft(draft)
                .versionNo(versionNo)
                .source(source)
                .sourceRef(sourceRef)
                .question(content.question())
                .description(content.description())
                .category(content.category())
                .difficulty(content.difficulty())
                .optionALabel(content.optionA().label())
                .optionARationale(content.optionA().rationale())
                .optionAPhilosopher(content.optionA().philosopher())
                .optionBLabel(content.optionB().label())
                .optionBRationale(content.optionB().rationale())
                .optionBPhilosopher(content.optionB().philosopher())
                .profileVersion(TopicPhilosopherProfiles.PROFILE_VERSION)
                .promptVersion(ClassTopicPrompts.PROMPT_VERSION)
                .build());
    }

    /** 같은 멱등키의 작업이 이미 있으면(환불 후 같은 순번 재요청) 그 행을 재사용해 attempt 를 누적한다 */
    private AiJob startJob(AiJobType type, String key, ClassTicket ticket, String model, LocalDateTime now) {
        AiJob job = aiJobRepository.findByIdempotencyKey(key)
                .orElseGet(() -> aiJobRepository.save(AiJob.builder()
                        .type(type)
                        .idempotencyKey(key)
                        .ticket(ticket)
                        .build()));
        job.start(model, now);
        return job;
    }

    /** start() 가 첫 호출을 세었으므로 나머지 호출만 더한다 */
    private static void addExtraAttempts(AiJob job, AiUsage usage) {
        for (int i = 1; i < usage.calls(); i++) {
            job.addAttempt();
        }
    }

    private static void succeedJob(AiJob job, AiUsage usage, LocalDateTime now) {
        addExtraAttempts(job, usage);
        job.succeed(usage.inputTokens(), usage.outputTokens(), now);
    }

    private static void finishJob(AiJob job, String errorCode, boolean rejected, AiUsage usage, LocalDateTime now) {
        addExtraAttempts(job, usage);
        if (rejected) {
            job.reject(errorCode, now);
        } else {
            job.fail(errorCode, now);
        }
    }
}

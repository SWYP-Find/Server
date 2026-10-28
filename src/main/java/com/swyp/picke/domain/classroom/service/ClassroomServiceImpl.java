package com.swyp.picke.domain.classroom.service;

import com.swyp.picke.domain.battle.entity.Battle;
import com.swyp.picke.domain.battle.enums.BattleStatus;
import com.swyp.picke.domain.battle.repository.BattleRepository;
import com.swyp.picke.domain.classroom.converter.ClassroomConverter;
import com.swyp.picke.domain.classroom.dto.request.ClassCreateRequest;
import com.swyp.picke.domain.classroom.dto.request.ClassUpdateRequest;
import com.swyp.picke.domain.classroom.dto.response.ClassDetailResponse;
import com.swyp.picke.domain.classroom.dto.response.ClassSummaryResponse;
import com.swyp.picke.domain.classroom.entity.ClassMember;
import com.swyp.picke.domain.classroom.entity.ClassTopicDraft;
import com.swyp.picke.domain.classroom.entity.ClassTopicVersion;
import com.swyp.picke.domain.classroom.entity.Classroom;
import com.swyp.picke.domain.classroom.enums.ClassBattleType;
import com.swyp.picke.domain.classroom.enums.ClassMemberRole;
import com.swyp.picke.domain.classroom.enums.ClassMemberStatus;
import com.swyp.picke.domain.classroom.enums.ClassStatus;
import com.swyp.picke.domain.classroom.repository.ClassMemberRepository;
import com.swyp.picke.domain.classroom.repository.ClassTopicDraftRepository;
import com.swyp.picke.domain.classroom.repository.ClassTopicVersionRepository;
import com.swyp.picke.domain.classroom.repository.ClassroomRepository;
import com.swyp.picke.domain.classroom.util.InviteCodeGenerator;
import com.swyp.picke.domain.ticket.entity.ClassTicket;
import com.swyp.picke.domain.ticket.enums.TicketStatus;
import com.swyp.picke.domain.ticket.repository.ClassTicketRepository;
import com.swyp.picke.domain.user.entity.User;
import com.swyp.picke.domain.user.repository.UserRepository;
import com.swyp.picke.global.common.exception.CustomException;
import com.swyp.picke.global.common.exception.ErrorCode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 클래스 생성/조회/수정/삭제.
 * <ul>
 *   <li>생성은 사용 중(ACTIVE, 기간 내) 이용권이 있어야 하고, 이용권 1개로 만들 수 있는 클래스 수는 제한 없음</li>
 *   <li>배틀은 기존 콘텐츠(battleId) 또는 검증된 커스텀 주제(topicVersionId) 중 하나. 커스텀이면 클래스 전용 배틀을 새로 만든다</li>
 *   <li>정원은 생성 시점 이용권 capacity 복사. 운영자가 참여하면 운영자도 1석 차지</li>
 *   <li>마감 여부는 상태가 아니라 min(deadline, 이용권 종료일) 로 조회 시 판정</li>
 *   <li>개설 후 수정은 이름 변경, 마감일 연장만</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClassroomServiceImpl implements ClassroomService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final int INVITE_CODE_MAX_ATTEMPTS = 10;
    /** 같은 생성 요청으로 보는 시간 범위. 연속 탭·네트워크 재전송을 잡을 만큼만 짧게 둔다. */
    private static final long DUPLICATE_WINDOW_SECONDS = 10;

    private final ClassroomRepository classroomRepository;
    private final ClassMemberRepository classMemberRepository;
    private final ClassTicketRepository classTicketRepository;
    private final BattleRepository battleRepository;
    private final ClassTopicVersionRepository classTopicVersionRepository;
    private final ClassTopicDraftRepository classTopicDraftRepository;
    private final ClassCustomBattleFactory customBattleFactory;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public ClassDetailResponse create(Long userId, ClassCreateRequest request) {
        LocalDateTime now = LocalDateTime.now(KST);
        User owner = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        ClassTicket ticket = findUsableTicket(userId, now);
        if ((request.battleId() == null) == (request.topicVersionId() == null)) {
            throw new CustomException(ErrorCode.CLASS_BATTLE_SOURCE_INVALID);
        }
        String requestKey = request.requestKey() == null || request.requestKey().isBlank() ? null : request.requestKey();
        String requestHash = requestKey == null ? null : requestHashOf(request);
        // 같은 이용권의 생성 요청은 행 잠금으로 한 번에 하나씩 처리한 뒤, 이미 만든 같은 요청의 클래스가 있으면 그것을 돌려준다
        ticket = classTicketRepository.findByIdForUpdate(ticket.getId()).orElse(ticket);
        Classroom duplicate = requestKey != null
                ? findByRequestKey(userId, requestKey, requestHash)
                : findRecentDuplicate(userId, request, now);
        if (duplicate != null) {
            log.info("클래스 중복 생성 요청 - 기존 클래스 반환 classId: {}, ownerId: {}", duplicate.getId(), userId);
            return ClassroomConverter.toDetail(duplicate, ClassMemberRole.OWNER, seatCountOf(duplicate), now);
        }
        ClassTopicVersion topicVersion = request.topicVersionId() == null ? null : findOwnTopicVersion(userId, request.topicVersionId());
        Battle tts = topicVersion == null ? findClassBattle(request.battleId()) : null;
        validateDeadlineRange(request.deadline(), ticket, now);
        Battle battle = tts != null ? tts : customBattleFactory.create(topicVersion, owner);

        Classroom classroom = classroomRepository.save(Classroom.builder()
                .name(request.name().strip())
                .inviteCode(newUniqueInviteCode())
                .owner(owner)
                .ticket(ticket)
                .battle(battle)
                .battleType(topicVersion == null ? ClassBattleType.TTS : ClassBattleType.CUSTOM)
                .topicVersion(topicVersion)
                .capacity(ticket.getCapacity())
                .deadline(request.deadline())
                .commentRequired(request.commentRequired())
                .ownerParticipates(request.ownerParticipates())
                .createRequestKey(requestKey)
                .createRequestHash(requestHash)
                .build());

        long memberCount = 0;
        if (request.ownerParticipates()) {
            classMemberRepository.save(ClassMember.builder()
                    .classroom(classroom)
                    .user(owner)
                    .displayName(resolveOwnerDisplayName(request.ownerDisplayName(), owner))
                    .role(ClassMemberRole.OWNER)
                    .joinedAt(now)
                    .build());
            memberCount = 1;
        }
        log.info("클래스 생성 - classId: {}, ownerId: {}, ticketId: {}, battleId: {}, capacity: {}",
                classroom.getId(), userId, ticket.getId(), battle.getId(), classroom.getCapacity());
        return ClassroomConverter.toDetail(classroom, ClassMemberRole.OWNER, memberCount, now);
    }

    @Override
    public List<ClassSummaryResponse> getMyClasses(Long userId) {
        LocalDateTime now = LocalDateTime.now(KST);
        List<Classroom> classrooms = classroomRepository.findMyClasses(userId);
        Map<Long, Long> seatCounts = seatCountsOf(classrooms);
        return classrooms.stream()
                .map(c -> ClassroomConverter.toSummary(
                        c, roleOf(c, userId), seatCounts.getOrDefault(c.getId(), 0L), now))
                .toList();
    }

    @Override
    public ClassDetailResponse getDetail(Long userId, Long classId) {
        LocalDateTime now = LocalDateTime.now(KST);
        Classroom classroom = findActiveClass(classId);
        ClassMemberRole role = roleOf(classroom, userId);
        if (role == null) {
            throw new CustomException(ErrorCode.CLASS_MEMBER_ONLY);
        }
        return ClassroomConverter.toDetail(classroom, role, seatCountOf(classroom), now);
    }

    @Override
    @Transactional
    public ClassDetailResponse update(Long userId, Long classId, ClassUpdateRequest request) {
        LocalDateTime now = LocalDateTime.now(KST);
        Classroom classroom = findOwnedClass(classId, userId);
        if (classroom.isClosed(now)) {
            throw new CustomException(ErrorCode.CLASS_CLOSED);
        }
        if (request.name() != null) {
            String name = request.name().strip();
            if (name.isEmpty()) {
                throw new CustomException(ErrorCode.COMMON_INVALID_PARAMETER);
            }
            classroom.rename(name);
        }
        if (request.deadline() != null) {
            validateDeadlineExtension(request.deadline(), classroom, now);
            classroom.extendDeadline(request.deadline());
        }
        return ClassroomConverter.toDetail(classroom, ClassMemberRole.OWNER, seatCountOf(classroom), now);
    }

    @Override
    @Transactional
    public void delete(Long userId, Long classId) {
        LocalDateTime now = LocalDateTime.now(KST);
        Classroom classroom = findOwnedClass(classId, userId);
        classroom.delete(now);
        log.info("클래스 삭제 - classId: {}, ownerId: {}", classId, userId);
    }

    // ===== 조회 헬퍼 =====

    private Classroom findActiveClass(Long classId) {
        return classroomRepository.findByIdAndStatus(classId, ClassStatus.ACTIVE)
                .orElseThrow(() -> new CustomException(ErrorCode.CLASS_NOT_FOUND));
    }

    private Classroom findOwnedClass(Long classId, Long userId) {
        Classroom classroom = findActiveClass(classId);
        if (!isOwner(classroom, userId)) {
            throw new CustomException(ErrorCode.CLASS_OWNER_ONLY);
        }
        return classroom;
    }

    private static boolean isOwner(Classroom classroom, Long userId) {
        return Objects.equals(classroom.getOwner().getId(), userId);
    }

    /** 운영자면 OWNER, ACTIVE 멤버면 MEMBER, 둘 다 아니면 null */
    private ClassMemberRole roleOf(Classroom classroom, Long userId) {
        if (isOwner(classroom, userId)) {
            return ClassMemberRole.OWNER;
        }
        return classMemberRepository
                .findByClassroomIdAndUserIdAndStatus(classroom.getId(), userId, ClassMemberStatus.ACTIVE)
                .map(ClassMember::getRole)
                .orElse(null);
    }

    private long seatCountOf(Classroom classroom) {
        return classMemberRepository.countByClassroomIdAndSeatHeldTrue(classroom.getId());
    }

    private Map<Long, Long> seatCountsOf(List<Classroom> classrooms) {
        Map<Long, Long> result = new HashMap<>();
        if (classrooms.isEmpty()) {
            return result;
        }
        List<Long> ids = classrooms.stream().map(Classroom::getId).toList();
        for (Object[] row : classMemberRepository.countSeatsHeldByClassroomIds(ids)) {
            result.put((Long) row[0], (Long) row[1]);
        }
        return result;
    }

    // ===== 생성 검증 =====

    /**
     * 요청 키로 이미 만든 클래스를 찾는다. 키가 같은데 내용이 다르면 클라이언트가 키를 재사용한 것이라 409.
     * 키는 운영자 단위로 유일해서 다른 운영자의 키와는 겹쳐도 상관없다.
     */
    private Classroom findByRequestKey(Long userId, String requestKey, String requestHash) {
        Classroom existing = classroomRepository.findByOwnerIdAndCreateRequestKey(userId, requestKey).orElse(null);
        if (existing == null) {
            return null;
        }
        if (!requestHash.equals(existing.getCreateRequestHash())) {
            throw new CustomException(ErrorCode.CLASS_REQUEST_KEY_CONFLICT);
        }
        if (existing.getStatus() != ClassStatus.ACTIVE) {
            throw new CustomException(ErrorCode.CLASS_DELETED);
        }
        return existing;
    }

    /** 같은 요청인지 비교할 본문 해시. 서버가 정규화하는 값(이름·운영자 표시 이름 앞뒤 공백)은 정규화한 뒤 비교한다 */
    static String requestHashOf(ClassCreateRequest request) {
        String canonical = String.join("\n",
                request.name().strip(),
                String.valueOf(request.battleId()),
                String.valueOf(request.topicVersionId()),
                String.valueOf(request.deadline()),
                String.valueOf(request.commentRequired()),
                String.valueOf(request.ownerParticipates()),
                request.ownerDisplayName() == null ? "" : request.ownerDisplayName().strip());
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(canonical.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    /**
     * 요청 키 없이 연속 탭·재전송으로 들어온 같은 생성 요청인지 판정한다.
     * 같은 운영자·같은 이름·같은 배틀(또는 주제 버전)으로 {@link #DUPLICATE_WINDOW_SECONDS}초 안에 만든 클래스가 있으면 그 클래스.
     * 이용권 기간 안에서 클래스 생성 수는 무제한이라, 이름이나 배틀이 다르면 새로 만든다.
     */
    private Classroom findRecentDuplicate(Long userId, ClassCreateRequest request, LocalDateTime now) {
        String name = request.name().strip();
        return classroomRepository.findByOwnerIdAndNameAndStatusAndCreatedAtAfterOrderByIdDesc(
                        userId, name, ClassStatus.ACTIVE, now.minusSeconds(DUPLICATE_WINDOW_SECONDS))
                .stream()
                .filter(c -> isSameBattleSource(c, request))
                .findFirst()
                .orElse(null);
    }

    private static boolean isSameBattleSource(Classroom classroom, ClassCreateRequest request) {
        if (request.topicVersionId() != null) {
            return classroom.getTopicVersion() != null && request.topicVersionId().equals(classroom.getTopicVersion().getId());
        }
        return classroom.getTopicVersion() == null && request.battleId().equals(classroom.getBattle().getId());
    }

    /**
     * 사용 중(ACTIVE) 이용권이 없거나 기간이 끝났으면 생성 불가.
     * 기간이 끝난 ACTIVE 를 EXPIRED 로 내리는 건 이용권 조회/등록(ClassTicketService) 몫이다.
     * 여기서 바꾸면 거절 예외로 트랜잭션이 롤백돼 어차피 남지 않는다.
     */
    private ClassTicket findUsableTicket(Long userId, LocalDateTime now) {
        return classTicketRepository.findByRegisteredUserIdAndStatus(userId, TicketStatus.ACTIVE)
                .filter(ticket -> ticket.isUsable(now))
                .orElseThrow(() -> new CustomException(ErrorCode.CLASS_TICKET_REQUIRED));
    }

    /**
     * 커스텀 배틀용 주제 버전. 본인 초안의 버전만 쓸 수 있고, 같은 초안으로는 클래스를 하나만 만든다 (PRD §16 draft_id 고유).
     * 동시 생성은 초안 행 잠금으로 직렬화하고, 마지막 방어선은 classes.topic_draft_id 유니크 제약이다.
     */
    private ClassTopicVersion findOwnTopicVersion(Long userId, Long topicVersionId) {
        ClassTopicVersion version = classTopicVersionRepository.findById(topicVersionId)
                .orElseThrow(() -> new CustomException(ErrorCode.CLASS_TOPIC_VERSION_NOT_FOUND));
        ClassTopicDraft draft = classTopicDraftRepository.findByIdForUpdate(version.getDraft().getId())
                .filter(d -> d.isOwnedBy(userId))
                .orElseThrow(() -> new CustomException(ErrorCode.CLASS_TOPIC_VERSION_NOT_FOUND));
        if (classroomRepository.existsByTopicDraftId(draft.getId())) {
            throw new CustomException(ErrorCode.CLASS_TOPIC_DRAFT_USED);
        }
        return version;
    }

    /** 기존 콘텐츠(TTS) 배틀. 공개(PUBLISHED) 상태이고 삭제되지 않은 배틀만 붙일 수 있다. */
    private Battle findClassBattle(Long battleId) {
        Battle battle = battleRepository.findById(battleId)
                .orElseThrow(() -> new CustomException(ErrorCode.BATTLE_NOT_FOUND));
        if (battle.getDeletedAt() != null || battle.getStatus() != BattleStatus.PUBLISHED) {
            throw new CustomException(ErrorCode.CLASS_BATTLE_NOT_AVAILABLE);
        }
        return battle;
    }

    /** deadline 은 "현재 시각 이후 ~ 이용권 종료일" 범위만 허용. null 은 마감 없음 */
    private static void validateDeadlineRange(LocalDateTime deadline, ClassTicket ticket, LocalDateTime now) {
        if (deadline == null) {
            return;
        }
        if (!deadline.isAfter(now) || deadline.isAfter(ticket.getExpiresAt())) {
            throw new CustomException(ErrorCode.CLASS_INVALID_DEADLINE);
        }
    }

    /** 마감일은 연장만. 기존 마감일이 없으면(= 이용권 종료일까지) 더 뒤로 갈 수 없으므로 거절 */
    private static void validateDeadlineExtension(LocalDateTime deadline, Classroom classroom, LocalDateTime now) {
        validateDeadlineRange(deadline, classroom.getTicket(), now);
        LocalDateTime current = classroom.getDeadline();
        if (current == null || !deadline.isAfter(current)) {
            throw new CustomException(ErrorCode.CLASS_DEADLINE_NOT_EXTENDED);
        }
    }

    private String newUniqueInviteCode() {
        for (int i = 0; i < INVITE_CODE_MAX_ATTEMPTS; i++) {
            String code = InviteCodeGenerator.generate();
            if (!classroomRepository.existsByInviteCode(code)) {
                return code;
            }
        }
        throw new CustomException(ErrorCode.CLASS_INVITE_CODE_GENERATION_FAILED);
    }

    private static String resolveOwnerDisplayName(String requested, User owner) {
        String name = requested == null ? "" : requested.strip();
        if (name.isEmpty()) {
            name = owner.getNickname() == null ? "운영자" : owner.getNickname().strip();
        }
        return name.length() > ClassMember.DISPLAY_NAME_MAX_LENGTH
                ? name.substring(0, ClassMember.DISPLAY_NAME_MAX_LENGTH)
                : name;
    }
}

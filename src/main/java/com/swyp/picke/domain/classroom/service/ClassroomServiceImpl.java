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
import com.swyp.picke.domain.classroom.entity.Classroom;
import com.swyp.picke.domain.classroom.enums.ClassBattleType;
import com.swyp.picke.domain.classroom.enums.ClassMemberRole;
import com.swyp.picke.domain.classroom.enums.ClassMemberStatus;
import com.swyp.picke.domain.classroom.enums.ClassStatus;
import com.swyp.picke.domain.classroom.repository.ClassMemberRepository;
import com.swyp.picke.domain.classroom.repository.ClassroomRepository;
import com.swyp.picke.domain.classroom.util.InviteCodeGenerator;
import com.swyp.picke.domain.ticket.entity.ClassTicket;
import com.swyp.picke.domain.ticket.enums.TicketStatus;
import com.swyp.picke.domain.ticket.repository.ClassTicketRepository;
import com.swyp.picke.domain.user.entity.User;
import com.swyp.picke.domain.user.repository.UserRepository;
import com.swyp.picke.global.common.exception.CustomException;
import com.swyp.picke.global.common.exception.ErrorCode;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
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

    private final ClassroomRepository classroomRepository;
    private final ClassMemberRepository classMemberRepository;
    private final ClassTicketRepository classTicketRepository;
    private final BattleRepository battleRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public ClassDetailResponse create(Long userId, ClassCreateRequest request) {
        LocalDateTime now = LocalDateTime.now(KST);
        User owner = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        ClassTicket ticket = findUsableTicket(userId, now);
        Battle battle = findClassBattle(request.battleId());
        validateDeadlineRange(request.deadline(), ticket, now);

        Classroom classroom = classroomRepository.save(Classroom.builder()
                .name(request.name().strip())
                .inviteCode(newUniqueInviteCode())
                .owner(owner)
                .ticket(ticket)
                .battle(battle)
                .battleType(ClassBattleType.TTS)
                .capacity(ticket.getCapacity())
                .deadline(request.deadline())
                .commentRequired(request.commentRequired())
                .ownerParticipates(request.ownerParticipates())
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

    /** 사용 중(ACTIVE) 이용권이 없거나 기간이 끝났으면 생성 불가. 끝난 ACTIVE 는 EXPIRED 로 내린다. */
    private ClassTicket findUsableTicket(Long userId, LocalDateTime now) {
        ClassTicket ticket = classTicketRepository.findByRegisteredUserIdAndStatus(userId, TicketStatus.ACTIVE)
                .orElseThrow(() -> new CustomException(ErrorCode.CLASS_TICKET_REQUIRED));
        if (!ticket.isUsable(now)) {
            ticket.expire();
            throw new CustomException(ErrorCode.CLASS_TICKET_REQUIRED);
        }
        return ticket;
    }

    /** 1차는 TTS(기존 콘텐츠) 만. 공개(PUBLISHED) 상태이고 삭제되지 않은 배틀만 붙일 수 있다. */
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

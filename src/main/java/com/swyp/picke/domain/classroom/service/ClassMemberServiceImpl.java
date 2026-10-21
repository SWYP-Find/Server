package com.swyp.picke.domain.classroom.service;

import com.swyp.picke.domain.classroom.converter.ClassMemberConverter;
import com.swyp.picke.domain.classroom.converter.ClassroomConverter;
import com.swyp.picke.domain.classroom.dto.request.ClassJoinRequest;
import com.swyp.picke.domain.classroom.dto.request.ClassMemberNameUpdateRequest;
import com.swyp.picke.domain.classroom.dto.response.ClassDetailResponse;
import com.swyp.picke.domain.classroom.dto.response.ClassJoinPreviewResponse;
import com.swyp.picke.domain.classroom.dto.response.ClassMemberResponse;
import com.swyp.picke.domain.classroom.entity.ClassMember;
import com.swyp.picke.domain.classroom.entity.Classroom;
import com.swyp.picke.domain.classroom.enums.ClassMemberRole;
import com.swyp.picke.domain.classroom.enums.ClassMemberStatus;
import com.swyp.picke.domain.classroom.enums.ClassStatus;
import com.swyp.picke.domain.classroom.repository.ClassMemberRepository;
import com.swyp.picke.domain.classroom.repository.ClassroomRepository;
import com.swyp.picke.domain.user.entity.User;
import com.swyp.picke.domain.user.repository.UserBattleRepository;
import com.swyp.picke.domain.user.repository.UserRepository;
import com.swyp.picke.global.common.exception.CustomException;
import com.swyp.picke.global.common.exception.ErrorCode;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 클래스 참여(초대코드)와 멤버 관리.
 * <ul>
 *   <li>참여는 클래스 행을 잠근 뒤 정원 검사 → 멤버 생성까지 한 트랜잭션 (마지막 1석 동시 신청은 1명만)</li>
 *   <li>정원 판정 = COUNT(seatHeld) &lt; capacity. 나가도 활동 이력이 있으면 좌석은 남는다</li>
 *   <li>같은 계정이 이미 참여 중이면 거절 대신 기존 상세를 돌려준다 (PRD "재시도하면 멤버 수를 늘리지 않고 기존 상세로")</li>
 *   <li>LEFT 는 같은 행을 되살려 재참여, REMOVED 는 영구 차단, 운영자는 나가기·내보내기 대상이 아니다</li>
 *   <li>코드 확인(previewJoin)은 참여와 같은 순서로 거절 사유를 판정하되, 이미 참여 중이면 409 로 알려준다 (Figma 코드 입력 단계 팝업)</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClassMemberServiceImpl implements ClassMemberService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final ClassroomRepository classroomRepository;
    private final ClassMemberRepository classMemberRepository;
    private final UserRepository userRepository;
    private final UserBattleRepository userBattleRepository;

    @Override
    @Transactional
    public ClassDetailResponse join(Long userId, ClassJoinRequest request) {
        LocalDateTime now = LocalDateTime.now(KST);
        String inviteCode = normalizeInviteCode(request.inviteCode());
        String displayName = normalizeDisplayName(request.displayName());

        Classroom classroom = classroomRepository.findByInviteCodeAndStatusForUpdate(inviteCode, ClassStatus.ACTIVE)
                .orElseThrow(() -> codeNotUsable(inviteCode));
        if (isOwner(classroom, userId)) {
            throw new CustomException(ErrorCode.CLASS_OWNER_CANNOT_JOIN);
        }

        Optional<ClassMember> existing = classMemberRepository.findByClassroomIdAndUserId(classroom.getId(), userId);
        if (existing.isPresent() && existing.get().isActive()) {
            return ClassroomConverter.toDetail(classroom, ClassMemberRole.MEMBER, seatCountOf(classroom), now);
        }
        rejectRemovedOrClosed(classroom, existing, now);
        rejectDuplicateDisplayName(classroom.getId(), displayName);
        rejectFull(classroom, existing);

        if (existing.isPresent()) {
            existing.get().rejoin(displayName, now);
        } else {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
            classMemberRepository.save(ClassMember.builder()
                    .classroom(classroom)
                    .user(user)
                    .displayName(displayName)
                    .role(ClassMemberRole.MEMBER)
                    .joinedAt(now)
                    .build());
        }
        log.info("클래스 참여 - classId: {}, userId: {}, rejoin: {}", classroom.getId(), userId, existing.isPresent());
        return ClassroomConverter.toDetail(classroom, ClassMemberRole.MEMBER, seatCountOf(classroom), now);
    }

    @Override
    public ClassJoinPreviewResponse previewJoin(Long userId, String rawInviteCode) {
        LocalDateTime now = LocalDateTime.now(KST);
        String inviteCode = normalizeInviteCode(rawInviteCode);
        Classroom classroom = classroomRepository.findByInviteCode(inviteCode)
                .orElseThrow(() -> new CustomException(ErrorCode.CLASS_INVITE_CODE_NOT_FOUND));
        if (classroom.getStatus() != ClassStatus.ACTIVE) {
            throw new CustomException(ErrorCode.CLASS_DELETED);
        }
        if (isOwner(classroom, userId)) {
            throw new CustomException(ErrorCode.CLASS_OWNER_CANNOT_JOIN);
        }
        Optional<ClassMember> existing = classMemberRepository.findByClassroomIdAndUserId(classroom.getId(), userId);
        if (existing.isPresent() && existing.get().isActive()) {
            throw new CustomException(ErrorCode.CLASS_ALREADY_JOINED);
        }
        rejectRemovedOrClosed(classroom, existing, now);
        rejectFull(classroom, existing);

        return new ClassJoinPreviewResponse(
                inviteCode,
                classroom.getName(),
                classroom.getBattle().getTitle(),
                classroom.getBattle().getThumbnailUrl(),
                classroom.getBattleType(),
                seatCountOf(classroom),
                classroom.getCapacity(),
                classroom.getEffectiveCloseAt());
    }

    @Override
    public List<ClassMemberResponse> getMembers(Long userId, Long classId) {
        Classroom classroom = findActiveClass(classId);
        if (!isOwner(classroom, userId) && findActiveMember(classId, userId).isEmpty()) {
            throw new CustomException(ErrorCode.CLASS_MEMBER_ONLY);
        }
        return classMemberRepository.findActiveMembers(classId).stream()
                .map(m -> ClassMemberConverter.toResponse(m, userId))
                .toList();
    }

    @Override
    @Transactional
    public ClassMemberResponse changeMyDisplayName(Long userId, Long classId, ClassMemberNameUpdateRequest request) {
        findActiveClass(classId);
        ClassMember me = findActiveMember(classId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.CLASS_MEMBER_ONLY));
        String displayName = normalizeDisplayName(request.displayName());
        if (!displayName.equals(me.getDisplayName())) {
            rejectDuplicateDisplayName(classId, displayName);
            me.changeDisplayName(displayName);
        }
        return ClassMemberConverter.toResponse(me, userId);
    }

    @Override
    @Transactional
    public void leave(Long userId, Long classId) {
        LocalDateTime now = LocalDateTime.now(KST);
        Classroom classroom = findActiveClass(classId);
        if (isOwner(classroom, userId)) {
            throw new CustomException(ErrorCode.CLASS_OWNER_CANNOT_LEAVE);
        }
        ClassMember me = findActiveMember(classId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.CLASS_MEMBER_ONLY));
        me.leave(now, hasActivity(me));
        log.info("클래스 나가기 - classId: {}, userId: {}, seatHeld: {}", classId, userId, me.isSeatHeld());
    }

    @Override
    @Transactional
    public void removeMember(Long userId, Long classId, Long memberId) {
        LocalDateTime now = LocalDateTime.now(KST);
        Classroom classroom = findActiveClass(classId);
        if (!isOwner(classroom, userId)) {
            throw new CustomException(ErrorCode.CLASS_OWNER_ONLY);
        }
        ClassMember target = classMemberRepository.findByIdAndClassroomId(memberId, classId)
                .filter(ClassMember::isActive)
                .orElseThrow(() -> new CustomException(ErrorCode.CLASS_MEMBER_NOT_FOUND));
        if (target.isOwner()) {
            throw new CustomException(ErrorCode.CLASS_OWNER_CANNOT_LEAVE);
        }
        target.remove(now, hasActivity(target));
        log.info("클래스 내보내기 - classId: {}, ownerId: {}, memberId: {}, seatHeld: {}",
                classId, userId, memberId, target.isSeatHeld());
    }

    // ===== 검증 =====

    /** 공백·하이픈 제거, 대문자 변환 후 6자가 아니면 400 (PRD S08). 이용권 코드(TicketCodeHasher)와 같은 규칙 */
    static String normalizeInviteCode(String raw) {
        String code = raw == null ? "" : raw.replaceAll("[\\s-]", "").toUpperCase(Locale.ROOT);
        if (code.length() != Classroom.INVITE_CODE_LENGTH) {
            throw new CustomException(ErrorCode.COMMON_INVALID_PARAMETER);
        }
        return code;
    }

    /** 앞뒤 공백 제거 후 1~20자 */
    static String normalizeDisplayName(String raw) {
        String name = raw == null ? "" : raw.strip();
        if (name.isEmpty() || name.length() > ClassMember.DISPLAY_NAME_MAX_LENGTH) {
            throw new CustomException(ErrorCode.COMMON_INVALID_PARAMETER);
        }
        return name;
    }

    /** 삭제된 클래스의 코드는 "없는 코드"와 구분한다 (Figma 참여 에러 "삭제된 클래스") */
    private CustomException codeNotUsable(String inviteCode) {
        return classroomRepository.existsByInviteCodeAndStatus(inviteCode, ClassStatus.DELETED)
                ? new CustomException(ErrorCode.CLASS_DELETED)
                : new CustomException(ErrorCode.CLASS_INVITE_CODE_NOT_FOUND);
    }

    /** 내보내진 멤버, 이용 종료(이용권 만료·회수), 입장 마감(마감일 경과)을 이 순서로 구분해 거절 (PRD §15) */
    private static void rejectRemovedOrClosed(Classroom classroom, Optional<ClassMember> existing, LocalDateTime now) {
        if (existing.isPresent() && existing.get().getStatus() == ClassMemberStatus.REMOVED) {
            throw new CustomException(ErrorCode.CLASS_MEMBER_REMOVED);
        }
        if (classroom.isTicketEnded(now)) {
            throw new CustomException(ErrorCode.CLASS_TICKET_ENDED);
        }
        if (classroom.isClosed(now)) {
            throw new CustomException(ErrorCode.CLASS_CLOSED);
        }
    }

    /** LEFT 상태로 좌석을 쥐고 있던 멤버의 재참여는 좌석을 새로 쓰지 않는다 */
    private void rejectFull(Classroom classroom, Optional<ClassMember> existing) {
        boolean needsSeat = existing.map(m -> !m.isSeatHeld()).orElse(true);
        if (needsSeat && seatCountOf(classroom) >= classroom.getCapacity()) {
            throw new CustomException(ErrorCode.CLASS_FULL);
        }
    }

    private void rejectDuplicateDisplayName(Long classId, String displayName) {
        if (classMemberRepository.existsByClassroomIdAndDisplayNameAndStatus(classId, displayName, ClassMemberStatus.ACTIVE)) {
            throw new CustomException(ErrorCode.CLASS_DISPLAY_NAME_DUPLICATED);
        }
    }

    /**
     * 사전투표 이후 활동이 있으면 나가도 좌석을 돌려주지 않는다 (PRD F15, ERD D04).
     * 클래스 안에서 사전투표하면 user_battles 에 class_id 행이 생기므로 그 존재 여부로 판단한다.
     */
    private boolean hasActivity(ClassMember member) {
        return userBattleRepository.existsByUserIdAndClassroomId(member.getUser().getId(), member.getClassroom().getId());
    }

    // ===== 조회 헬퍼 =====

    private Classroom findActiveClass(Long classId) {
        return classroomRepository.findByIdAndStatus(classId, ClassStatus.ACTIVE)
                .orElseThrow(() -> new CustomException(ErrorCode.CLASS_NOT_FOUND));
    }

    private Optional<ClassMember> findActiveMember(Long classId, Long userId) {
        return classMemberRepository.findByClassroomIdAndUserIdAndStatus(classId, userId, ClassMemberStatus.ACTIVE);
    }

    private static boolean isOwner(Classroom classroom, Long userId) {
        return Objects.equals(classroom.getOwner().getId(), userId);
    }

    private long seatCountOf(Classroom classroom) {
        return classMemberRepository.countByClassroomIdAndSeatHeldTrue(classroom.getId());
    }
}

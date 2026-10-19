package com.swyp.picke.domain.classroom.service;

import com.swyp.picke.domain.classroom.entity.ClassMember;
import com.swyp.picke.domain.classroom.entity.Classroom;
import com.swyp.picke.domain.classroom.enums.ClassMemberRole;
import com.swyp.picke.domain.classroom.enums.ClassMemberStatus;
import com.swyp.picke.domain.classroom.enums.ClassStatus;
import com.swyp.picke.domain.classroom.repository.ClassMemberRepository;
import com.swyp.picke.domain.classroom.repository.ClassroomRepository;
import com.swyp.picke.global.common.exception.CustomException;
import com.swyp.picke.global.common.exception.ErrorCode;
import java.time.LocalDateTime;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 클래스 안의 배틀 진행(투표·의견·댓글)에 대한 접근 판정. 투표/의견 서비스와 클래스 배틀 서비스가 공유한다.
 * <ul>
 *   <li>읽기(상세·통계·의견·댓글 목록): 운영자 또는 ACTIVE 멤버. 마감 후에도 가능 (PRD 부록 D)</li>
 *   <li>쓰기(투표·의견·댓글 등록/수정/삭제): ACTIVE 멤버 행이 있는 사람만. 참여하지 않는 운영자는 403 (PRD §2, F09)</li>
 *   <li>쓰기는 effective_close_at 이전 서버 접수만 허용. 이용권 종료 409 CLASS_409_TICKET_ENDED, 마감일 경과 409 CLASS_409_CLOSED (PRD §15)</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClassAccessService {

    private final ClassroomRepository classroomRepository;
    private final ClassMemberRepository classMemberRepository;

    /** 내 역할(없으면 null)과 ACTIVE 멤버 행(없으면 null). 운영자는 ownerParticipates=true 일 때만 멤버 행이 있다 */
    public record ClassAccess(Classroom classroom, ClassMemberRole role, ClassMember member) {
        public boolean canRead() {
            return role != null;
        }

        public boolean canWrite() {
            return member != null;
        }
    }

    public ClassAccess resolve(Long classId, Long userId) {
        Classroom classroom = classroomRepository.findByIdAndStatus(classId, ClassStatus.ACTIVE)
                .orElseThrow(() -> new CustomException(ErrorCode.CLASS_NOT_FOUND));
        return resolve(classroom, userId);
    }

    public ClassAccess resolve(Classroom classroom, Long userId) {
        if (classroom.getStatus() != ClassStatus.ACTIVE) {
            throw new CustomException(ErrorCode.CLASS_NOT_FOUND);
        }
        ClassMember member = classMemberRepository
                .findByClassroomIdAndUserIdAndStatus(classroom.getId(), userId, ClassMemberStatus.ACTIVE)
                .orElse(null);
        ClassMemberRole role;
        if (Objects.equals(classroom.getOwner().getId(), userId)) {
            role = ClassMemberRole.OWNER;
        } else {
            role = member != null ? member.getRole() : null;
        }
        return new ClassAccess(classroom, role, member);
    }

    public ClassAccess requireReadable(Long classId, Long userId) {
        return requireReadable(resolve(classId, userId));
    }

    public ClassAccess requireReadable(Classroom classroom, Long userId) {
        return requireReadable(resolve(classroom, userId));
    }

    public ClassAccess requireWritable(Long classId, Long userId, LocalDateTime now) {
        return requireWritable(resolve(classId, userId), now);
    }

    public ClassAccess requireWritable(Classroom classroom, Long userId, LocalDateTime now) {
        return requireWritable(resolve(classroom, userId), now);
    }

    private static ClassAccess requireReadable(ClassAccess access) {
        if (!access.canRead()) {
            throw new CustomException(ErrorCode.CLASS_MEMBER_ONLY);
        }
        return access;
    }

    private static ClassAccess requireWritable(ClassAccess access, LocalDateTime now) {
        if (!access.canWrite()) {
            throw new CustomException(ErrorCode.CLASS_MEMBER_ONLY);
        }
        Classroom classroom = access.classroom();
        if (classroom.isTicketEnded(now)) {
            throw new CustomException(ErrorCode.CLASS_TICKET_ENDED);
        }
        if (classroom.isClosed(now)) {
            throw new CustomException(ErrorCode.CLASS_CLOSED);
        }
        return access;
    }
}

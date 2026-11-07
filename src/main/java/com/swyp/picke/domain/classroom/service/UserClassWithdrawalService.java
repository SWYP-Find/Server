package com.swyp.picke.domain.classroom.service;

import com.swyp.picke.domain.classroom.entity.ClassMember;
import com.swyp.picke.domain.classroom.entity.Classroom;
import com.swyp.picke.domain.classroom.enums.ClassMemberStatus;
import com.swyp.picke.domain.classroom.enums.ClassStatus;
import com.swyp.picke.domain.classroom.repository.ClassMemberRepository;
import com.swyp.picke.domain.classroom.repository.ClassroomRepository;
import com.swyp.picke.domain.perspective.enums.PerspectiveStatus;
import com.swyp.picke.domain.perspective.repository.PerspectiveCommentRepository;
import com.swyp.picke.domain.perspective.repository.PerspectiveRepository;
import com.swyp.picke.domain.user.entity.User;
import com.swyp.picke.domain.user.repository.UserBattleRepository;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Applies account-withdrawal policy to live class memberships and authored discussion content. */
@Service
@RequiredArgsConstructor
public class UserClassWithdrawalService {

    private final ClassroomRepository classroomRepository;
    private final ClassMemberRepository classMemberRepository;
    private final UserBattleRepository userBattleRepository;
    private final PerspectiveRepository perspectiveRepository;
    private final PerspectiveCommentRepository perspectiveCommentRepository;

    @Transactional
    public void apply(User user, LocalDateTime now) {
        Long userId = user.getId();

        // A deleted owner cannot continue operating an active class. Keep historical records for reports/audit.
        List<Classroom> ownedClasses = classroomRepository.findByOwnerIdAndStatus(userId, ClassStatus.ACTIVE);
        ownedClasses.forEach(classroom -> classroom.delete(now));

        // Match the normal leave rule: a member who has started the class keeps their seat consumed.
        List<ClassMember> activeMemberships = classMemberRepository.findByUserIdAndStatus(userId, ClassMemberStatus.ACTIVE);
        Set<Long> activeClassroomIds = activeMemberships.stream()
                .map(member -> member.getClassroom().getId()).collect(java.util.stream.Collectors.toSet());
        Set<Long> classroomsWithActivity = activeClassroomIds.isEmpty() ? Set.of()
                : userBattleRepository.findClassroomIdsWithActivity(userId, activeClassroomIds);
        for (ClassMember member : activeMemberships) {
            boolean hasActivity = classroomsWithActivity.contains(member.getClassroom().getId());
            member.leave(now, hasActivity);
        }

        // Hide authored text from public and class screens while preserving rows referenced by historical records.
        perspectiveRepository.hideAllByUserId(userId, PerspectiveStatus.HIDDEN);
        perspectiveCommentRepository.hideAllByUserId(userId);
    }
}

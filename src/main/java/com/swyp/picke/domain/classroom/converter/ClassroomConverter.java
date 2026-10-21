package com.swyp.picke.domain.classroom.converter;

import com.swyp.picke.domain.battle.entity.Battle;
import com.swyp.picke.domain.classroom.dto.response.ClassDetailResponse;
import com.swyp.picke.domain.classroom.dto.response.ClassSummaryResponse;
import com.swyp.picke.domain.classroom.entity.Classroom;
import com.swyp.picke.domain.classroom.enums.ClassMemberRole;
import java.time.LocalDateTime;

public final class ClassroomConverter {

    private ClassroomConverter() {
    }

    public static ClassSummaryResponse toSummary(Classroom classroom, ClassMemberRole myRole, long memberCount,
                                                 LocalDateTime now) {
        Battle battle = classroom.getBattle();
        return new ClassSummaryResponse(
                classroom.getId(),
                classroom.getName(),
                myRole,
                battle.getId(),
                battle.getTitle(),
                battle.getThumbnailUrl(),
                classroom.getBattleType(),
                memberCount,
                classroom.getCapacity(),
                classroom.getEffectiveCloseAt(),
                classroom.isClosed(now),
                classroom.getCreatedAt()
        );
    }

    /** 초대코드는 운영자에게만 내려간다. */
    public static ClassDetailResponse toDetail(Classroom classroom, ClassMemberRole myRole, long memberCount,
                                               LocalDateTime now) {
        Battle battle = classroom.getBattle();
        boolean owner = myRole == ClassMemberRole.OWNER;
        return new ClassDetailResponse(
                classroom.getId(),
                classroom.getName(),
                owner ? classroom.getInviteCode() : null,
                myRole,
                classroom.getOwner().getId(),
                new ClassDetailResponse.BattleInfo(
                        battle.getId(), battle.getTitle(), battle.getSummary(), battle.getThumbnailUrl()),
                classroom.getBattleType(),
                classroom.getTopicVersion() == null ? null : classroom.getTopicVersion().getId(),
                memberCount,
                classroom.getCapacity(),
                classroom.getDeadline(),
                classroom.getTicket().getExpiresAt(),
                classroom.getEffectiveCloseAt(),
                classroom.isClosed(now),
                classroom.isCommentRequired(),
                classroom.isOwnerParticipates(),
                classroom.getCreatedAt()
        );
    }
}

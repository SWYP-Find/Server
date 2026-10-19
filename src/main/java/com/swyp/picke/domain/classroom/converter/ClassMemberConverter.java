package com.swyp.picke.domain.classroom.converter;

import com.swyp.picke.domain.classroom.dto.response.ClassMemberResponse;
import com.swyp.picke.domain.classroom.entity.ClassMember;
import com.swyp.picke.domain.user.entity.User;
import java.util.Objects;

public final class ClassMemberConverter {

    private ClassMemberConverter() {
    }

    public static ClassMemberResponse toResponse(ClassMember member, Long viewerId) {
        User user = member.getUser();
        return new ClassMemberResponse(
                member.getId(),
                member.getDisplayName(),
                member.getRole(),
                user.getCharacterUrl(),
                Objects.equals(user.getId(), viewerId),
                member.getJoinedAt()
        );
    }
}

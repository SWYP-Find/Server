package com.swyp.picke.domain.classroom.converter;

import com.swyp.picke.domain.classroom.dto.response.ClassMemberResponse;
import com.swyp.picke.domain.classroom.entity.ClassMember;
import com.swyp.picke.domain.user.entity.User;
import java.util.Objects;

public final class ClassMemberConverter {

    private ClassMemberConverter() {
    }

    /** avatarUrl 은 회원 캐릭터 이미지 URL이다 ({@code ClassImageUrlResolver#avatar}). */
    public static ClassMemberResponse toResponse(ClassMember member, Long viewerId, String avatarUrl) {
        User user = member.getUser();
        return new ClassMemberResponse(
                member.getId(),
                member.getDisplayName(),
                member.getRole(),
                avatarUrl,
                Objects.equals(user.getId(), viewerId),
                member.getJoinedAt()
        );
    }
}

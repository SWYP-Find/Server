package com.swyp.picke.domain.classroom.converter;

import com.swyp.picke.domain.classroom.dto.response.ClassMemberResponse;
import com.swyp.picke.domain.classroom.entity.ClassMember;
import com.swyp.picke.domain.classroom.entity.Classroom;
import com.swyp.picke.domain.classroom.enums.ClassMemberRole;
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
                member.getJoinedAt(),
                true
        );
    }

    /**
     * 참여하지 않는 운영자(ownerParticipates = false)는 멤버 행이 없지만 명단 맨 위에 보여준다 (#665).
     * 좌석·명단 인원·결과 통계에는 들어가지 않는다.
     */
    public static ClassMemberResponse toNonParticipatingOwner(Classroom classroom, Long viewerId, String avatarUrl) {
        User owner = classroom.getOwner();
        String nickname = owner.getNickname() == null || owner.getNickname().isBlank() ? "운영자" : owner.getNickname().strip();
        return new ClassMemberResponse(
                null,
                nickname,
                ClassMemberRole.OWNER,
                avatarUrl,
                Objects.equals(owner.getId(), viewerId),
                classroom.getCreatedAt(),
                false
        );
    }
}

package com.swyp.picke.domain.classroom.converter;

import com.swyp.picke.domain.classroom.dto.response.ClassMemberResponse;
import com.swyp.picke.domain.classroom.entity.ClassMember;
import com.swyp.picke.domain.classroom.entity.Classroom;
import com.swyp.picke.domain.classroom.enums.ClassMemberRole;
import com.swyp.picke.domain.user.entity.User;
import java.util.Objects;

public final class ClassMemberConverter {

    /** 미참여 운영자 줄의 memberId. 실제 멤버십 ID 는 1부터라 겹치지 않는다 */
    public static final long NON_PARTICIPATING_OWNER_MEMBER_ID = 0L;

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
     * 참여하지 않는 운영자(ownerParticipates = false)는 멤버 행이 없지만, 운영자 본인이 명단을 볼 때는 맨 위에 보여준다 (#665).
     * 앱이 memberId 를 필수 값으로 읽어서 null 대신 {@link #NON_PARTICIPATING_OWNER_MEMBER_ID} 를 쓴다.
     * 좌석·명단 인원·결과 통계에는 들어가지 않는다.
     */
    public static ClassMemberResponse toNonParticipatingOwner(Classroom classroom, String avatarUrl) {
        User owner = classroom.getOwner();
        String nickname = owner.getNickname() == null || owner.getNickname().isBlank() ? "운영자" : owner.getNickname().strip();
        return new ClassMemberResponse(
                NON_PARTICIPATING_OWNER_MEMBER_ID,
                nickname,
                ClassMemberRole.OWNER,
                avatarUrl,
                true,
                classroom.getCreatedAt(),
                false
        );
    }
}

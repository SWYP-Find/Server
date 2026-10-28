package com.swyp.picke.domain.classroom.service;

import com.swyp.picke.domain.classroom.entity.ClassMember;
import com.swyp.picke.domain.classroom.entity.Classroom;
import com.swyp.picke.domain.classroom.enums.ClassMemberStatus;
import com.swyp.picke.domain.classroom.repository.ClassMemberRepository;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 클래스 안의 의견·댓글 작성자를 계정 닉네임 대신 클래스 표시 이름으로 보여주기 위한 조회기.
 * 일반 배틀(classroom == null)이면 빈 맵을 돌려주므로 호출 측은 기존 닉네임을 그대로 쓴다.
 * 클래스에서는 계정 식별자(userTag)를 내리지 않고, 나간·내보낸 멤버의 글은 목록에서 숨긴다.
 */
@Component
@RequiredArgsConstructor
public class ClassDisplayNameResolver {

    private final ClassMemberRepository classMemberRepository;

    /** userId → 클래스 표시 이름(ACTIVE 멤버만). 목록 화면에서 작성자 수만큼 쿼리가 나가지 않도록 한 번에 조회한다 */
    public Map<Long, String> resolve(Classroom classroom, Collection<Long> userIds) {
        if (classroom == null || userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        List<ClassMember> members = classMemberRepository.findByClassroomIdAndUserIdInAndStatus(
                classroom.getId(), userIds, ClassMemberStatus.ACTIVE);
        Map<Long, String> names = new HashMap<>();
        for (ClassMember member : members) {
            names.put(member.getUser().getId(), member.getDisplayName());
        }
        return names;
    }

    /** 단건용. 클래스가 아니거나 멤버 행이 없으면 fallback(계정 닉네임)을 쓴다 */
    public String nameOf(Classroom classroom, Long userId, String fallback) {
        return resolve(classroom, List.of(userId)).getOrDefault(userId, fallback);
    }

    /** 목록에 보여줄 작성자인지. 일반 배틀이면 항상 true, 클래스면 resolve 결과에 있는(ACTIVE) 멤버만 */
    public static boolean isVisible(Classroom classroom, Map<Long, String> names, Long userId) {
        return classroom == null || names.containsKey(userId);
    }

    /** 단건용 isVisible */
    public boolean isVisibleAuthor(Classroom classroom, Long userId) {
        return isVisible(classroom, resolve(classroom, List.of(userId)), userId);
    }

    /** 클래스에서는 계정 식별자(userTag)를 숨긴다 */
    public static String userTagOf(Classroom classroom, String userTag) {
        return classroom == null ? userTag : null;
    }
}

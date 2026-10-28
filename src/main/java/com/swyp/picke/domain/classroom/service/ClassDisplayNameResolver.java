package com.swyp.picke.domain.classroom.service;

import com.swyp.picke.domain.classroom.entity.ClassMember;
import com.swyp.picke.domain.classroom.entity.Classroom;
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
 */
@Component
@RequiredArgsConstructor
public class ClassDisplayNameResolver {

    private final ClassMemberRepository classMemberRepository;

    /** userId → 클래스 표시 이름. 목록 화면에서 작성자 수만큼 쿼리가 나가지 않도록 한 번에 조회한다 */
    public Map<Long, String> resolve(Classroom classroom, Collection<Long> userIds) {
        if (classroom == null || userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        List<ClassMember> members = classMemberRepository.findByClassroomIdAndUserIdIn(classroom.getId(), userIds);
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
}

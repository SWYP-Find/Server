package com.swyp.picke.domain.user.repository;

import com.swyp.picke.domain.battle.entity.Battle;
import com.swyp.picke.domain.classroom.entity.Classroom;
import com.swyp.picke.domain.user.entity.User;
import com.swyp.picke.domain.user.entity.UserBattle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserBattleRepository extends JpaRepository<UserBattle, Long> {

    // 특정 유저가 특정 배틀에 참여한 기록 찾기. classroom 이 null 이면 일반 참여(class_id IS NULL), 아니면 해당 클래스 내 참여
    Optional<UserBattle> findByUserAndBattleAndClassroom(User user, Battle battle, Classroom classroom);

    // 클래스 내 활동 이력(사전투표 이후) 존재 여부 — 나가기/내보내기 시 좌석 유지 판정(F15)
    boolean existsByUserIdAndClassroomId(Long userId, Long classroomId);
}

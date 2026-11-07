package com.swyp.picke.domain.vote.repository;

import com.swyp.picke.domain.battle.entity.Battle;
import com.swyp.picke.domain.battle.entity.BattleOption;
import com.swyp.picke.domain.classroom.entity.Classroom;
import com.swyp.picke.domain.classroom.enums.ClassMemberStatus;
import com.swyp.picke.domain.user.entity.User;
import com.swyp.picke.domain.vote.entity.BattleVote;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * votes 는 (user_id, battle_id, class_id) 로 유일하다.
 * 일반 배틀 참여는 class_id IS NULL, 클래스 내 참여는 class_id = 클래스 id 로 저장되므로
 * 모든 조회는 반드시 class 스코프를 명시한다. (한 유저가 같은 배틀을 일반+클래스로 참여하면 행이 2개)
 *
 * 파생 쿼리의 Classroom 파라미터에 null 을 넘기면 Spring Data 가 "class_id IS NULL" 로 바꿔서 실행한다.
 */
public interface BattleVoteRepository extends JpaRepository<BattleVote, Long> {

    // ===== 일반 배틀 전용 (class_id IS NULL) =====

    List<BattleVote> findAllByBattleAndClassroomIsNull(Battle battle);

    Optional<BattleVote> findByBattleIdAndUserIdAndClassroomIsNull(Long battleId, Long userId);

    @Query("SELECT v FROM BattleVote v LEFT JOIN FETCH v.postVoteOption " +
            "WHERE v.battle.id = :battleId AND v.user.id = :userId AND v.classroom IS NULL")
    Optional<BattleVote> findByBattleIdAndUserIdWithOption(@Param("battleId") Long battleId, @Param("userId") Long userId);

    @Query("SELECT v FROM BattleVote v JOIN FETCH v.battle LEFT JOIN FETCH v.postVoteOption " +
            "WHERE v.user.id = :userId AND v.battle.id IN :battleIds AND v.classroom IS NULL")
    List<BattleVote> findByUserIdAndBattleIdInWithPostVoteOption(
            @Param("userId") Long userId,
            @Param("battleIds") List<Long> battleIds
    );

    long countByBattleAndClassroomIsNullAndPreVoteOption(Battle battle, BattleOption preVoteOption);

    // ===== 일반/클래스 공용 (classroom == null 이면 일반, 아니면 해당 클래스) =====

    Optional<BattleVote> findByBattleAndUserAndClassroom(Battle battle, User user, Classroom classroom);

    long countByBattleAndClassroom(Battle battle, Classroom classroom);

    long countByBattleAndClassroomAndPostVoteOption(Battle battle, Classroom classroom, BattleOption postVoteOption);

    long countByBattleAndClassroomAndPostVoteOptionIsNotNull(Battle battle, Classroom classroom);

    /** 클래스 투표 통계: 지금 클래스에 남아 있는(status) 멤버의 사후투표만 선택지별로 센다. [optionId, count] */
    @Query("SELECT v.postVoteOption.id, COUNT(v) FROM BattleVote v WHERE v.battle = :battle AND v.classroom = :classroom " +
            "AND v.postVoteOption IS NOT NULL AND EXISTS (SELECT m.id FROM ClassMember m " +
            "WHERE m.classroom = v.classroom AND m.user = v.user AND m.status = :status) GROUP BY v.postVoteOption.id")
    List<Object[]> countClassPostVotesByMemberStatus(@Param("battle") Battle battle, @Param("classroom") Classroom classroom,
                                                     @Param("status") ClassMemberStatus status);

    Optional<BattleVote> findTopByBattleAndClassroomOrderByUpdatedAtDesc(Battle battle, Classroom classroom);

    // 클래스 결과 집계용: 클래스 안의 투표 전체
    List<BattleVote> findByClassroomId(Long classroomId);

    // ===== 마이페이지·성향·추천: 일반 배틀 기록만 =====

    // 내 배틀 기록: 사후투표까지 완료한 기록만 노출
    @Query("SELECT v FROM BattleVote v JOIN FETCH v.battle JOIN FETCH v.preVoteOption JOIN FETCH v.postVoteOption " +
           "WHERE v.user.id = :userId AND v.classroom IS NULL AND v.postVoteOption IS NOT NULL ORDER BY v.createdAt DESC")
    List<BattleVote> findByUserIdAndPostVoteOptionIsNotNullOrderByCreatedAtDesc(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT v FROM BattleVote v JOIN FETCH v.battle JOIN FETCH v.preVoteOption JOIN FETCH v.postVoteOption " +
           "WHERE v.user.id = :userId AND v.classroom IS NULL AND v.preVoteOption.displayOrder = :displayOrder AND v.postVoteOption IS NOT NULL ORDER BY v.createdAt DESC")
    List<BattleVote> findByUserIdAndPreVoteOptionDisplayOrderAndPostVoteOptionIsNotNullOrderByCreatedAtDesc(
            @Param("userId") Long userId, @Param("displayOrder") Integer displayOrder, Pageable pageable);

    @Query("SELECT v FROM BattleVote v JOIN FETCH v.battle JOIN FETCH v.preVoteOption JOIN FETCH v.postVoteOption " +
           "WHERE v.user.id = :userId AND v.classroom IS NULL AND v.preVoteOption.displayOrder <> :displayOrder AND v.postVoteOption IS NOT NULL ORDER BY v.createdAt DESC")
    List<BattleVote> findByUserIdAndPreVoteOptionDisplayOrderNotAndPostVoteOptionIsNotNullOrderByCreatedAtDesc(
            @Param("userId") Long userId, @Param("displayOrder") Integer displayOrder, Pageable pageable);

    @Query("SELECT COUNT(v) FROM BattleVote v WHERE v.user.id = :userId AND v.classroom IS NULL AND v.postVoteOption IS NOT NULL")
    long countByUserIdAndPostVoteOptionIsNotNull(@Param("userId") Long userId);

    @Query("SELECT COUNT(v) FROM BattleVote v WHERE v.user.id = :userId AND v.classroom IS NULL AND v.preVoteOption.displayOrder = :displayOrder AND v.postVoteOption IS NOT NULL")
    long countByUserIdAndPreVoteOptionDisplayOrderAndPostVoteOptionIsNotNull(@Param("userId") Long userId, @Param("displayOrder") Integer displayOrder);

    @Query("SELECT COUNT(v) FROM BattleVote v WHERE v.user.id = :userId AND v.classroom IS NULL AND v.preVoteOption.displayOrder <> :displayOrder AND v.postVoteOption IS NOT NULL")
    long countByUserIdAndPreVoteOptionDisplayOrderNotAndPostVoteOptionIsNotNull(@Param("userId") Long userId, @Param("displayOrder") Integer displayOrder);

    // countTotalParticipation 등 "사전투표만 해도 참여로 집계"하는 통계용 — postVoteOption 필터 없음
    long countByUserIdAndClassroomIsNull(Long userId);

    // 오늘의 배틀 무료 참여(일 1회) 제한 체크용: 유저가 오늘 날짜인 배틀에 이미 참여했는지 확인 (클래스 참여는 무료진입 판정에서 제외)
    boolean existsByUserIdAndClassroomIsNullAndBattle_TargetDate(Long userId, LocalDate targetDate);

    @Query("SELECT COUNT(v) FROM BattleVote v WHERE v.user.id = :userId AND v.classroom IS NULL " +
            "AND v.postVoteOption IS NOT NULL " +
            "AND v.preVoteOption <> v.postVoteOption")
    long countOpinionChangesByUserId(@Param("userId") Long userId);

    List<BattleVote> findByUserIdAndClassroomIsNull(Long userId);

    // MypageService: 철학자 유형 산출용 - 최초 N개 투표 조회 (생성순)
    @Query("SELECT v FROM BattleVote v JOIN FETCH v.battle WHERE v.user.id = :userId AND v.classroom IS NULL ORDER BY v.createdAt ASC")
    List<BattleVote> findByUserIdOrderByCreatedAtAsc(@Param("userId") Long userId, Pageable pageable);

    // 추천용: 유저가 참여한 배틀 ID 조회
    @Query("SELECT v.battle.id FROM BattleVote v WHERE v.user.id = :userId AND v.classroom IS NULL")
    List<Long> findParticipatedBattleIdsByUserId(@Param("userId") Long userId);

    // 추천용: 특정 배틀에 참여한 유저 ID 조회
    @Query("SELECT DISTINCT v.user.id FROM BattleVote v WHERE v.battle.id IN :battleIds AND v.classroom IS NULL")
    List<Long> findUserIdsByBattleIds(@Param("battleIds") List<Long> battleIds);

    // 추천용: 특정 유저들이 참여한 배틀 ID 조회
    @Query("SELECT v.battle.id FROM BattleVote v WHERE v.user.id IN :userIds AND v.classroom IS NULL")
    List<Long> findParticipatedBattleIdsByUserIds(@Param("userIds") List<Long> userIds);
}

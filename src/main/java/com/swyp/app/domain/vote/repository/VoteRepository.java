package com.swyp.app.domain.vote.repository;

import com.swyp.app.domain.battle.entity.Battle;
import com.swyp.app.domain.battle.entity.BattleOption;
import com.swyp.app.domain.battle.enums.BattleOptionLabel;
import com.swyp.app.domain.user.entity.User;
import com.swyp.app.domain.vote.entity.Vote;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface VoteRepository extends JpaRepository<Vote, Long> {

    // ScenarioService : Battle 엔티티 조회 없이 ID만으로 투표 내역 확인
    Optional<Vote> findByBattleIdAndUserId(Long battleId, Long userId);

    // VoteService : 이미 조회된 Battle 엔티티를 활용하여 투표 내역 확인
    Optional<Vote> findByBattleAndUserId(Battle battle, Long userId);

    long countByBattle(Battle battle);

    long countByBattleAndPreVoteOption(Battle battle, BattleOption preVoteOption);

    Optional<Vote> findTopByBattleOrderByUpdatedAtDesc(Battle battle);

    // MypageService: 사용자 투표 기록 조회 (offset 페이지네이션)
    @Query("SELECT v FROM Vote v JOIN FETCH v.battle JOIN FETCH v.preVoteOption " +
           "WHERE v.userId = :userId ORDER BY v.createdAt DESC")
    List<Vote> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId, Pageable pageable);

    // MypageService: 사용자 투표 기록 - voteSide(PRO/CON) 필터
    @Query("SELECT v FROM Vote v JOIN FETCH v.battle JOIN FETCH v.preVoteOption " +
           "WHERE v.userId = :userId AND v.preVoteOption.label = :label ORDER BY v.createdAt DESC")
    List<Vote> findByUserIdAndPreVoteOptionLabelOrderByCreatedAtDesc(
            @Param("userId") Long userId, @Param("label") BattleOptionLabel label, Pageable pageable);

    // MypageService: 사용자 투표 전체 수
    long countByUserId(Long userId);

    // MypageService: 사용자 투표 수 - voteSide 필터
    @Query("SELECT COUNT(v) FROM Vote v WHERE v.userId = :userId AND v.preVoteOption.label = :label")
    long countByUserIdAndPreVoteOptionLabel(@Param("userId") Long userId, @Param("label") BattleOptionLabel label);

    // MypageService (recap): 사후 투표 완료 수
    long countByUserIdAndStatus(Long userId, com.swyp.app.domain.vote.enums.VoteStatus status);

    // MypageService (recap): 입장 변경 수 (사전/사후 투표 옵션이 다른 경우)
    @Query("SELECT COUNT(v) FROM Vote v WHERE v.userId = :userId AND v.status = 'POST_VOTED' " +
           "AND v.preVoteOption <> v.postVoteOption")
    long countOpinionChangesByUserId(@Param("userId") Long userId);

    // MypageService (recap): 사용자가 참여한 모든 투표 (배틀 목록 추출용)
    List<Vote> findByUserId(Long userId);

    // MypageService: 철학자 유형 산출용 - 최초 N개 투표 조회 (생성순)
    @Query("SELECT v FROM Vote v JOIN FETCH v.battle WHERE v.userId = :userId ORDER BY v.createdAt ASC")
    List<Vote> findByUserIdOrderByCreatedAtAsc(@Param("userId") Long userId, Pageable pageable);
}

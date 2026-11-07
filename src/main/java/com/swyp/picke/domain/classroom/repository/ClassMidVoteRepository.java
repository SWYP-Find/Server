package com.swyp.picke.domain.classroom.repository;

import com.swyp.picke.domain.classroom.entity.ClassMidVote;
import com.swyp.picke.domain.vote.entity.BattleVote;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClassMidVoteRepository extends JpaRepository<ClassMidVote, Long> {

    List<ClassMidVote> findByVoteOrderByIdAsc(BattleVote vote);

    Optional<ClassMidVote> findByVoteAndCheckpointId(BattleVote vote, Long checkpointId);

    /** 같은 투표의 중간투표 저장을 직렬화한다 */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from BattleVote v where v.id = :voteId")
    Optional<BattleVote> lockVote(@Param("voteId") Long voteId);

    @Query("select m from ClassMidVote m join fetch m.vote v join fetch v.user where v.classroom.id = :classId order by m.id")
    List<ClassMidVote> findByClassId(@Param("classId") Long classId);
}

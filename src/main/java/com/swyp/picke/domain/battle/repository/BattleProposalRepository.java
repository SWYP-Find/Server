package com.swyp.picke.domain.battle.repository;

import com.swyp.picke.domain.battle.entity.BattleProposal;
import com.swyp.picke.domain.battle.enums.BattleProposalStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BattleProposalRepository extends JpaRepository<BattleProposal, Long> {
    Page<BattleProposal> findAllByStatusOrderByCreatedAtDesc(BattleProposalStatus status, Pageable pageable);
    Page<BattleProposal> findAllByOrderByCreatedAtDesc(Pageable pageable);
    Page<BattleProposal> findAllByStatus(BattleProposalStatus status, Pageable pageable);
}

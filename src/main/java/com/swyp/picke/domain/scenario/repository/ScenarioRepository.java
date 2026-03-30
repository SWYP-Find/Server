package com.swyp.picke.domain.scenario.repository;

import com.swyp.picke.domain.scenario.entity.Scenario;
import com.swyp.picke.domain.scenario.enums.ScenarioStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ScenarioRepository extends JpaRepository<Scenario, Long> {
    Optional<Scenario> findByBattleIdAndStatus(Long battleId, ScenarioStatus status);
    Optional<Scenario> findByBattleId(Long battleId);
    boolean existsByBattleId(Long battleId);
}
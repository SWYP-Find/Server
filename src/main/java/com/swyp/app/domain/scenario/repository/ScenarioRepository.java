package com.swyp.app.domain.scenario.repository;

import com.swyp.app.domain.scenario.entity.Scenario;
import com.swyp.app.domain.scenario.enums.ScenarioStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ScenarioRepository extends JpaRepository<Scenario, Long> {
    Optional<Scenario> findByBattleIdAndStatus(Long battleId, ScenarioStatus status);
    boolean existsByBattleId(Long battleId);
}
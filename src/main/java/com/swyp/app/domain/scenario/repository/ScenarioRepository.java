package com.swyp.app.domain.scenario.repository;

import com.swyp.app.domain.scenario.entity.Scenario;
import com.swyp.app.domain.scenario.enums.ScenarioStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ScenarioRepository extends JpaRepository<Scenario, Long> {

    // 비동기 쓰레드에서도 모든 연관 데이터를 한 번에 가져오도록 설정
    @EntityGraph(attributePaths = {"nodes", "nodes.scripts", "nodes.options"})
    @Query("select s from Scenario s where s.id = :id")
    Optional<Scenario> findWithDetailsById(@Param("id") Long id);

    Optional<Scenario> findByBattleIdAndStatus(Long battleId, ScenarioStatus status);
    boolean existsByBattleId(Long battleId);
}
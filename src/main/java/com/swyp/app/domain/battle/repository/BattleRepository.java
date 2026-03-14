package com.swyp.app.domain.battle.repository;

import com.swyp.app.domain.battle.entity.Battle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface BattleRepository extends JpaRepository<Battle, UUID> {
}

package com.swyp.picke.domain.scenario.repository;

import com.swyp.picke.domain.scenario.entity.PhilosopherVoice;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PhilosopherVoiceRepository extends JpaRepository<PhilosopherVoice, Long> {
    Optional<PhilosopherVoice> findByName(String name);
    boolean existsByName(String name);
}

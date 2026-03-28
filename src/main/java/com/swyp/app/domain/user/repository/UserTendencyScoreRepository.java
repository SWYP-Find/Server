package com.swyp.app.domain.user.repository;

import com.swyp.app.domain.user.entity.UserTendencyScore;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserTendencyScoreRepository extends JpaRepository<UserTendencyScore, Long> {

    Optional<UserTendencyScore> findByUserId(Long userId);
}

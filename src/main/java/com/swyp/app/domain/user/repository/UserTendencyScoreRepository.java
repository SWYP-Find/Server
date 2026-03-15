package com.swyp.app.domain.user.repository;

import com.swyp.app.domain.user.entity.UserTendencyScore;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserTendencyScoreRepository extends JpaRepository<UserTendencyScore, Long> {
}

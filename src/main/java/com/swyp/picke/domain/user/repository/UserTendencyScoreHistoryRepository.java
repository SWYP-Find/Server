package com.swyp.picke.domain.user.repository;

import com.swyp.picke.domain.user.entity.User;
import com.swyp.picke.domain.user.entity.UserTendencyScoreHistory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserTendencyScoreHistoryRepository extends JpaRepository<UserTendencyScoreHistory, Long> {
    List<UserTendencyScoreHistory> findByUserOrderByIdDesc(User user, Pageable pageable);
    List<UserTendencyScoreHistory> findByUserAndIdLessThanOrderByIdDesc(User user, Long id, Pageable pageable);
}

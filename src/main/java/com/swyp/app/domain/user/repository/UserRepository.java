package com.swyp.app.domain.user.repository;

import com.swyp.app.domain.user.entity.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUserTag(String userTag);
    Optional<User> findTopByOrderByIdDesc();
    boolean existsByUserTag(String userTag);
}

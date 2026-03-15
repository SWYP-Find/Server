package com.swyp.app.domain.user.repository;

import com.swyp.app.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUserTag(String userTag);
    Optional<User> findTopByOrderByIdDesc();
    boolean existsByUserTag(String userTag);
}

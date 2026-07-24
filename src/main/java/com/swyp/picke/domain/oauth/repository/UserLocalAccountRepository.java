package com.swyp.picke.domain.oauth.repository;

import com.swyp.picke.domain.oauth.entity.UserLocalAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserLocalAccountRepository extends JpaRepository<UserLocalAccount, Long> {

    Optional<UserLocalAccount> findByUsername(String username);

    boolean existsByUsername(String username);
}

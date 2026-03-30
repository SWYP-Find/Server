package com.swyp.picke.domain.oauth.repository;

import com.swyp.picke.domain.oauth.entity.AuthRefreshToken;
import com.swyp.picke.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AuthRefreshTokenRepository extends JpaRepository<AuthRefreshToken, Long> {

    Optional<AuthRefreshToken> findByTokenHash(String tokenHash);

    void deleteByUser(User user);
}
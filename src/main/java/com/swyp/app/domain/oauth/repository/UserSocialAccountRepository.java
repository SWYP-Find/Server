package com.swyp.app.domain.oauth.repository;

import com.swyp.app.domain.oauth.entity.UserSocialAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserSocialAccountRepository extends JpaRepository<UserSocialAccount, Long> {

    Optional<UserSocialAccount> findByProviderAndProviderUserId(
            String provider, String providerUserId);
}
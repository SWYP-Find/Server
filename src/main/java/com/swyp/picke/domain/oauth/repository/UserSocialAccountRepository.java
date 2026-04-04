package com.swyp.picke.domain.oauth.repository;

import com.swyp.picke.domain.oauth.entity.UserSocialAccount;
import com.swyp.picke.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserSocialAccountRepository extends JpaRepository<UserSocialAccount, Long> {

    Optional<UserSocialAccount> findByProviderAndProviderUserId(
            String provider, String providerUserId);

    Optional<UserSocialAccount> findByUser(User user);

    void deleteByUser(User user);
}
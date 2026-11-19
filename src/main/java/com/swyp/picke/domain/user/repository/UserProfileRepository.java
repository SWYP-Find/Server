package com.swyp.picke.domain.user.repository;

import com.swyp.picke.domain.user.entity.UserProfile;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {

    Optional<UserProfile> findByUserId(Long userId);

    Optional<UserProfile> findByRecapShareKey(String recapShareKey);

    List<UserProfile> findAllByUserIdIn(Collection<Long> userIds);
}

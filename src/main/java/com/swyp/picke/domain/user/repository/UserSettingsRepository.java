package com.swyp.picke.domain.user.repository;

import com.swyp.picke.domain.user.entity.UserSettings;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface UserSettingsRepository extends JpaRepository<UserSettings, Long> {

    Optional<UserSettings> findByUserId(Long userId);

    @Query("SELECT s.user.id FROM UserSettings s WHERE s.newBattleEnabled = true")
    List<Long> findUserIdsByNewBattleEnabledTrue();

    @Query("SELECT s.user.id FROM UserSettings s WHERE s.marketingEventEnabled = true")
    List<Long> findUserIdsByMarketingEventEnabledTrue();
}

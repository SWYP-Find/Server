package com.swyp.picke.domain.user.repository;

import com.swyp.picke.domain.user.entity.UserSettings;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserSettingsRepository extends JpaRepository<UserSettings, Long> {

    Optional<UserSettings> findByUserId(Long userId);
}

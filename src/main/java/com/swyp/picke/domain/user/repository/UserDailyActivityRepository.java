package com.swyp.picke.domain.user.repository;

import com.swyp.picke.domain.user.entity.UserDailyActivity;
import java.time.LocalDate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface UserDailyActivityRepository extends JpaRepository<UserDailyActivity, Long> {

    @Modifying
    @Transactional
    @Query(value = """
            INSERT INTO user_daily_activities (user_id, activity_date, logged_in, active, created_at, updated_at)
            VALUES (:userId, :date, false, true, now(), now())
            ON CONFLICT (user_id, activity_date)
            DO UPDATE SET active = true, updated_at = now()
            """, nativeQuery = true)
    void markActive(@Param("userId") Long userId, @Param("date") LocalDate date);

    @Modifying
    @Transactional
    @Query(value = """
            INSERT INTO user_daily_activities (user_id, activity_date, logged_in, active, created_at, updated_at)
            VALUES (:userId, :date, true, false, now(), now())
            ON CONFLICT (user_id, activity_date)
            DO UPDATE SET logged_in = true, updated_at = now()
            """, nativeQuery = true)
    void markLoggedIn(@Param("userId") Long userId, @Param("date") LocalDate date);
}

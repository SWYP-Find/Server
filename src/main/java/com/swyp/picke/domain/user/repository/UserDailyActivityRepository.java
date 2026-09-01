package com.swyp.picke.domain.user.repository;

import com.swyp.picke.domain.user.entity.UserDailyActivity;
import com.swyp.picke.domain.user.repository.projection.DailyUserCount;
import java.time.LocalDate;
import java.util.List;
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

    long countByActivityDateAndLoggedInTrue(LocalDate activityDate);

    long countByActivityDateAndActiveTrue(LocalDate activityDate);

    /**
     * from~to 구간의 일자별 DAU(그날 active=true인 유저 수). 활동이 없는 날짜도 0으로 채워서 반환한다.
     */
    @Query(value = """
            SELECT gs::date AS activity_date, COUNT(uda.user_id) AS count
            FROM generate_series(CAST(:from AS timestamp), CAST(:to AS timestamp), interval '1 day') AS gs
            LEFT JOIN user_daily_activities uda
                ON uda.activity_date = gs::date AND uda.active = true
            GROUP BY gs::date
            ORDER BY gs::date
            """, nativeQuery = true)
    List<DailyUserCount> findDailyActiveUserCounts(@Param("from") LocalDate from, @Param("to") LocalDate to);

    /**
     * from~to 구간의 일자별 MAU(그날 기준 최근 30일 롤링 윈도우의 distinct active 유저 수).
     */
    @Query(value = """
            SELECT gs::date AS activity_date, COUNT(DISTINCT uda.user_id) AS count
            FROM generate_series(CAST(:from AS timestamp), CAST(:to AS timestamp), interval '1 day') AS gs
            LEFT JOIN user_daily_activities uda
                ON uda.activity_date BETWEEN (gs::date - INTERVAL '29 days') AND gs::date AND uda.active = true
            GROUP BY gs::date
            ORDER BY gs::date
            """, nativeQuery = true)
    List<DailyUserCount> findRollingMonthlyActiveUserCounts(@Param("from") LocalDate from, @Param("to") LocalDate to);
}

package com.swyp.picke.domain.user.repository;

import com.swyp.picke.domain.user.entity.User;
import com.swyp.picke.domain.user.enums.UserRole;
import com.swyp.picke.domain.user.enums.UserStatus;
import com.swyp.picke.domain.user.repository.projection.DailyUserCount;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUserTag(String userTag);
    Optional<User> findTopByOrderByIdDesc();
    boolean existsByUserTag(String userTag);

    @Query("select u.credit from User u where u.id = :id")
    Integer findCreditById(@Param("id") Long id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update User u set u.credit = u.credit + :amount where u.id = :id")
    int incrementCredit(@Param("id") Long id, @Param("amount") int amount);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update User u set u.credit = u.credit - :amount where u.id = :id and u.credit >= :amount")
    int decrementCreditIfEnough(@Param("id") Long id, @Param("amount") int amount);

    List<User> findAllByStatus(UserStatus status);

    List<User> findAllByRole(UserRole role);

    @Query("""
            select u from User u
            where u.nickname like concat('%', :keyword, '%')
               or u.userTag like concat('%', :keyword, '%')
            order by u.id desc
            """)
    Slice<User> searchByNicknameOrUserTag(@Param("keyword") String keyword, Pageable pageable);

    long countByStatus(UserStatus status);

    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    /**
     * from~to 구간의 일자별 신규 가입자 수. 가입자가 없는 날짜도 0으로 채워서 반환한다.
     */
    @Query(value = """
            SELECT gs::date AS activity_date, COUNT(u.id) AS count
            FROM generate_series(CAST(:from AS timestamp), CAST(:to AS timestamp), interval '1 day') AS gs
            LEFT JOIN users u ON CAST(u.created_at AS date) = gs::date
            GROUP BY gs::date
            ORDER BY gs::date
            """, nativeQuery = true)
    List<DailyUserCount> findDailyNewUserCounts(@Param("from") LocalDate from, @Param("to") LocalDate to);

    /**
     * from~to 구간이 걸쳐있는 주(ISO 8601, 월요일 시작)별 신규 가입자 수. 가입자가 없는 주도 0으로 채워서 반환하며,
     * 반환되는 날짜는 각 주의 시작일(월요일)이다.
     */
    @Query(value = """
            SELECT gs::date AS activity_date, COUNT(u.id) AS count
            FROM generate_series(
                date_trunc('week', CAST(:from AS timestamp)),
                date_trunc('week', CAST(:to AS timestamp)),
                interval '1 week'
            ) AS gs
            LEFT JOIN users u ON date_trunc('week', u.created_at) = gs
            GROUP BY gs::date
            ORDER BY gs::date
            """, nativeQuery = true)
    List<DailyUserCount> findWeeklyNewUserCounts(@Param("from") LocalDate from, @Param("to") LocalDate to);
}

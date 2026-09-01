package com.swyp.picke.domain.user.repository;

import com.swyp.picke.domain.user.entity.User;
import com.swyp.picke.domain.user.enums.UserRole;
import com.swyp.picke.domain.user.enums.UserStatus;
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
}

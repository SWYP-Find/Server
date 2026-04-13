package com.swyp.picke.domain.user.repository;

import com.swyp.picke.domain.user.entity.User;
import com.swyp.picke.domain.user.enums.UserStatus;
import java.util.List;
import java.util.Optional;
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

    List<User> findAllByStatus(UserStatus status);
}

package com.swyp.picke.domain.user.repository;

import com.swyp.picke.domain.user.entity.UserWithdrawal;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserWithdrawalRepository extends JpaRepository<UserWithdrawal, Long> {

    boolean existsByUser_Id(Long userId);
}

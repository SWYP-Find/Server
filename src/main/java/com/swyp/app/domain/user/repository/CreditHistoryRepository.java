package com.swyp.app.domain.user.repository;

import com.swyp.app.domain.user.entity.CreditHistory;
import com.swyp.app.domain.user.enums.CreditType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CreditHistoryRepository extends JpaRepository<CreditHistory, Long> {

    @Query("SELECT COALESCE(SUM(c.amount), 0) FROM CreditHistory c WHERE c.user.id = :userId")
    int sumAmountByUserId(@Param("userId") Long userId);

    boolean existsByUserIdAndCreditTypeAndReferenceId(Long userId, CreditType creditType, Long referenceId);
}

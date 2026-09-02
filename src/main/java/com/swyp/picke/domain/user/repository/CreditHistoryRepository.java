package com.swyp.picke.domain.user.repository;

import com.swyp.picke.domain.user.entity.CreditHistory;
import com.swyp.picke.domain.user.enums.CreditType;
import com.swyp.picke.domain.user.repository.projection.CreditTypeStats;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CreditHistoryRepository extends JpaRepository<CreditHistory, Long> {

    /**
     * 유저의 모든 CreditHistory amount 합계.
     * User.credit 캐시가 도입된 이후 잔액 조회 경로에서는 사용하지 않는다.
     * 백필/검증 용도로만 유지.
     */
    @Query("SELECT COALESCE(SUM(c.amount), 0) FROM CreditHistory c WHERE c.user.id = :userId")
    int sumAmountByUserId(@Param("userId") Long userId);

    boolean existsByUserIdAndCreditTypeAndReferenceId(Long userId, CreditType creditType, Long referenceId);

    Page<CreditHistory> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    long countByCreditTypeAndCreatedAtBetween(CreditType creditType, LocalDateTime start, LocalDateTime end);

    /**
     * 어드민 대시보드용: from~to 구간의 크레딧 타입별 지급/차감 건수 및 총액.
     */
    @Query("""
            SELECT c.creditType AS creditType, COUNT(c) AS count, COALESCE(SUM(c.amount), 0) AS totalAmount
            FROM CreditHistory c
            WHERE c.createdAt >= :start AND c.createdAt < :end
            GROUP BY c.creditType
            ORDER BY c.creditType
            """)
    List<CreditTypeStats> findStatsByCreatedAtBetween(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );
}

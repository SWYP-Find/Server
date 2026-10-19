package com.swyp.picke.domain.ticket.repository;

import com.swyp.picke.domain.ticket.entity.ClassTicket;
import com.swyp.picke.domain.ticket.enums.TicketCodeStatus;
import com.swyp.picke.domain.ticket.enums.TicketStatus;
import java.util.Collection;
import java.util.List;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClassTicketRepository extends JpaRepository<ClassTicket, Long> {

    Optional<ClassTicket> findByCodeHash(String codeHash);

    boolean existsByCodeHash(String codeHash);

    /** 주문의 살아 있는(REVOKED 아닌) 이용권. 재발급 이력이 있어도 1건 (partial unique) */
    Optional<ClassTicket> findByFundingOrderIdAndStatusNot(Long fundingOrderId, TicketStatus status);

    List<ClassTicket> findByFundingOrderIdInAndStatusNot(Collection<Long> fundingOrderIds, TicketStatus status);

    long countByStatusNot(TicketStatus status);

    long countByCodeStatus(TicketCodeStatus codeStatus);

    long countByStatusIn(Collection<TicketStatus> statuses);

    /** 등록용: 같은 코드를 동시에 두 요청이 등록하지 못하도록 행 잠금(SELECT ... FOR UPDATE) */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from ClassTicket t where t.codeHash = :codeHash")
    Optional<ClassTicket> findByCodeHashForUpdate(@Param("codeHash") String codeHash);

    Optional<ClassTicket> findByRegisteredUserIdAndStatus(Long userId, TicketStatus status);

    /** 내 이용권 조회용: ACTIVE 가 있으면 그것, 없으면 가장 최근에 만료된 것 */
    Optional<ClassTicket> findTopByRegisteredUserIdAndStatusInOrderByActivatedAtDesc(
            Long userId, Collection<TicketStatus> statuses);
}

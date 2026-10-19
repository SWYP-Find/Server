package com.swyp.picke.domain.ticket.repository;

import com.swyp.picke.domain.ticket.entity.FundingOrder;
import com.swyp.picke.domain.ticket.enums.PaymentStatus;
import com.swyp.picke.domain.ticket.enums.TicketCodeStatus;
import com.swyp.picke.domain.ticket.enums.TicketStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FundingOrderRepository extends JpaRepository<FundingOrder, Long> {

    Optional<FundingOrder> findByOrderId(String orderId);

    List<FundingOrder> findByOrderIdIn(Collection<String> orderIds);

    /** 발급 대상: 결제 확정·수량 1·결제일 있음·살아 있는 이용권 없음 (PRD §30, CF08 재실행 안전) */
    @Query("""
            select o from FundingOrder o
            where o.paymentStatus = com.swyp.picke.domain.ticket.enums.PaymentStatus.PAID_CONFIRMED
              and o.quantity = 1
              and o.paidAt is not null
              and not exists (select 1 from ClassTicket t
                              where t.fundingOrder = o
                                and t.status <> com.swyp.picke.domain.ticket.enums.TicketStatus.REVOKED)
            order by o.id asc
            """)
    List<FundingOrder> findIssuable();

    @Query("""
            select count(o) from FundingOrder o
            where o.paymentStatus = com.swyp.picke.domain.ticket.enums.PaymentStatus.PAID_CONFIRMED
              and o.quantity = 1
              and o.paidAt is not null
              and not exists (select 1 from ClassTicket t
                              where t.fundingOrder = o
                                and t.status <> com.swyp.picke.domain.ticket.enums.TicketStatus.REVOKED)
            """)
    long countIssuable();

    /** 보류: 결제 확정이지만 수량이 1 이 아니거나 결제일이 없는 행 + 결제 대기·예약 */
    @Query("""
            select count(o) from FundingOrder o
            where (o.paymentStatus = com.swyp.picke.domain.ticket.enums.PaymentStatus.PAID_CONFIRMED
                     and (o.quantity <> 1 or o.paidAt is null))
               or o.paymentStatus in (com.swyp.picke.domain.ticket.enums.PaymentStatus.RESERVED,
                                      com.swyp.picke.domain.ticket.enums.PaymentStatus.PAYMENT_PENDING)
            """)
    long countHeld();

    long countByPaymentStatus(PaymentStatus paymentStatus);

    /**
     * 어드민 목록: 주문 + 살아 있는 이용권(없으면 null). 필터는 null 이면 무시.
     * 결과 Object[] = {FundingOrder, ClassTicket|null}
     */
    @Query(value = """
            select o, t from FundingOrder o
            left join ClassTicket t on t.fundingOrder = o
                 and t.status <> com.swyp.picke.domain.ticket.enums.TicketStatus.REVOKED
            left join fetch t.registeredUser
            where (:paymentStatus is null or o.paymentStatus = :paymentStatus)
              and (:ticketStatus is null or t.status = :ticketStatus)
              and (:codeStatus is null or t.codeStatus = :codeStatus)
              and (:keyword is null
                   or lower(o.orderId) like lower(concat('%', :keyword, '%'))
                   or lower(o.supporterName) like lower(concat('%', :keyword, '%'))
                   or lower(coalesce(o.supporterEmail, '')) like lower(concat('%', :keyword, '%')))
            order by o.id desc
            """,
            countQuery = """
            select count(o) from FundingOrder o
            left join ClassTicket t on t.fundingOrder = o
                 and t.status <> com.swyp.picke.domain.ticket.enums.TicketStatus.REVOKED
            where (:paymentStatus is null or o.paymentStatus = :paymentStatus)
              and (:ticketStatus is null or t.status = :ticketStatus)
              and (:codeStatus is null or t.codeStatus = :codeStatus)
              and (:keyword is null
                   or lower(o.orderId) like lower(concat('%', :keyword, '%'))
                   or lower(o.supporterName) like lower(concat('%', :keyword, '%'))
                   or lower(coalesce(o.supporterEmail, '')) like lower(concat('%', :keyword, '%')))
            """)
    Page<Object[]> searchWithLiveTicket(@Param("paymentStatus") PaymentStatus paymentStatus,
                                        @Param("ticketStatus") TicketStatus ticketStatus,
                                        @Param("codeStatus") TicketCodeStatus codeStatus,
                                        @Param("keyword") String keyword,
                                        Pageable pageable);
}

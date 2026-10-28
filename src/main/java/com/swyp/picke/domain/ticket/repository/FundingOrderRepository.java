package com.swyp.picke.domain.ticket.repository;

import com.swyp.picke.domain.ticket.entity.FundingOrder;
import com.swyp.picke.domain.ticket.enums.PaymentStatus;
import com.swyp.picke.domain.ticket.enums.RewardTier;
import com.swyp.picke.domain.ticket.enums.DeliveryStatus;
import com.swyp.picke.domain.ticket.enums.TicketCodeStatus;
import com.swyp.picke.domain.ticket.enums.TicketStatus;
import com.swyp.picke.domain.ticket.enums.FundingPlatform;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FundingOrderRepository extends JpaRepository<FundingOrder, Long> {

    Optional<FundingOrder> findByOrderId(String orderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from FundingOrder o where o.id = :id")
    Optional<FundingOrder> findByIdForUpdate(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from FundingOrder o where o.id in :ids order by o.id asc")
    List<FundingOrder> findAllByIdForUpdate(@Param("ids") Collection<Long> ids);

    List<FundingOrder> findByOrderIdIn(Collection<String> orderIds);

    /** 발급 대상: 결제 완료·결제일 있음·발급된 코드 수가 주문 수량보다 적음. */
    @Query("""
            select o from FundingOrder o
            where o.paymentStatus = com.swyp.picke.domain.ticket.enums.PaymentStatus.PAID_CONFIRMED
              and o.paidAt is not null
              and o.quantity > (select count(t) from ClassTicket t
                                where t.fundingOrder = o
                                  and t.status <> com.swyp.picke.domain.ticket.enums.TicketStatus.REVOKED)
            order by o.id asc
            """)
    List<FundingOrder> findIssuable();

    @Query("""
            select count(o) from FundingOrder o
            where o.paymentStatus = com.swyp.picke.domain.ticket.enums.PaymentStatus.PAID_CONFIRMED
              and o.paidAt is not null
              and o.quantity > (select count(t) from ClassTicket t
                                where t.fundingOrder = o
                                  and t.status <> com.swyp.picke.domain.ticket.enums.TicketStatus.REVOKED)
            """)
    long countIssuable();

    /** 보류: 결제 완료 행에 결제일이 없거나 결제 대기·예약 상태인 주문. */
    @Query("""
            select count(o) from FundingOrder o
            where (o.paymentStatus = com.swyp.picke.domain.ticket.enums.PaymentStatus.PAID_CONFIRMED
                     and o.paidAt is null)
               or o.paymentStatus in (com.swyp.picke.domain.ticket.enums.PaymentStatus.RESERVED,
                                      com.swyp.picke.domain.ticket.enums.PaymentStatus.PAYMENT_PENDING)
            """)
    long countHeld();

    long countByPaymentStatus(PaymentStatus paymentStatus);

    long countByPlatformAndCampaignId(FundingPlatform platform, String campaignId);

    long countByPlatformAndCampaignIdAndPaymentStatus(
            FundingPlatform platform, String campaignId, PaymentStatus paymentStatus);

    @Query("""
            select count(o) from FundingOrder o
            where o.platform = com.swyp.picke.domain.ticket.enums.FundingPlatform.TUMBLBUG
              and o.campaignId = :campaignId
              and exists (select t.id from ClassTicket t where t.fundingOrder = o
                          and t.status <> com.swyp.picke.domain.ticket.enums.TicketStatus.REVOKED)
              and not exists (select t.id from ClassTicket t where t.fundingOrder = o
                          and t.status <> com.swyp.picke.domain.ticket.enums.TicketStatus.REVOKED
                          and not exists (select a.id from TicketDeliveryAttempt a
                              where a.ticket = t
                                and a.status = com.swyp.picke.domain.ticket.enums.DeliveryStatus.SUCCESS
                                and a.id = (select max(latest.id) from TicketDeliveryAttempt latest where latest.ticket = t)))
            """)
    long countDeliveredByTumblbugCampaign(@Param("campaignId") String campaignId);

    /**
     * 어드민 목록: 주문 + 살아 있는 이용권(없으면 null). 필터는 null 이면 무시.
     * 결과 Object[] = {FundingOrder, ClassTicket|null}
     */
    @Query(value = """
            select o, t from FundingOrder o
            left join ClassTicket t on t.fundingOrder = o
                 and t.status <> com.swyp.picke.domain.ticket.enums.TicketStatus.REVOKED
            left join fetch t.registeredUser
            where (:campaignId is null or o.campaignId = :campaignId)
              and (:paymentStatus is null or o.paymentStatus = :paymentStatus)
              and (:ticketStatus is null or t.status = :ticketStatus)
              and (:codeStatus is null or t.codeStatus = :codeStatus)
              and (:rewardTier is null or o.rewardTier = :rewardTier)
              and (:deliveryPending = false or not exists (
                   select a.id from TicketDeliveryAttempt a where a.ticket = t))
              and (:deliveryStatus is null or exists (
                   select a.id from TicketDeliveryAttempt a
                   where a.ticket = t and a.status = :deliveryStatus
                     and a.id = (select max(latest.id) from TicketDeliveryAttempt latest where latest.ticket = t)))
              and (:keyword is null
                   or lower(o.orderId) like lower(concat('%', :keyword, '%'))
                   or lower(o.supporterName) like lower(concat('%', :keyword, '%'))
                   or lower(coalesce(o.supporterEmail, '')) like lower(concat('%', :keyword, '%'))
                   or o.supporterPhone = :keyword
                   or exists (select r.id from TumblbugImportRecord r where r.fundingOrder = o
                              and lower(r.supporterNumber) like lower(concat('%', :keyword, '%'))))
            order by o.id desc
            """,
            countQuery = """
            select count(o) from FundingOrder o
            left join ClassTicket t on t.fundingOrder = o
                 and t.status <> com.swyp.picke.domain.ticket.enums.TicketStatus.REVOKED
            where (:campaignId is null or o.campaignId = :campaignId)
              and (:paymentStatus is null or o.paymentStatus = :paymentStatus)
              and (:ticketStatus is null or t.status = :ticketStatus)
              and (:codeStatus is null or t.codeStatus = :codeStatus)
              and (:rewardTier is null or o.rewardTier = :rewardTier)
              and (:deliveryPending = false or not exists (
                   select a.id from TicketDeliveryAttempt a where a.ticket = t))
              and (:deliveryStatus is null or exists (
                   select a.id from TicketDeliveryAttempt a
                   where a.ticket = t and a.status = :deliveryStatus
                     and a.id = (select max(latest.id) from TicketDeliveryAttempt latest where latest.ticket = t)))
              and (:keyword is null
                   or lower(o.orderId) like lower(concat('%', :keyword, '%'))
                   or lower(o.supporterName) like lower(concat('%', :keyword, '%'))
                   or lower(coalesce(o.supporterEmail, '')) like lower(concat('%', :keyword, '%'))
                   or o.supporterPhone = :keyword
                   or exists (select r.id from TumblbugImportRecord r where r.fundingOrder = o
                              and lower(r.supporterNumber) like lower(concat('%', :keyword, '%'))))
            """)
    Page<Object[]> searchWithLiveTicket(@Param("paymentStatus") PaymentStatus paymentStatus,
                                        @Param("ticketStatus") TicketStatus ticketStatus,
                                        @Param("codeStatus") TicketCodeStatus codeStatus,
                                        @Param("campaignId") String campaignId,
                                        @Param("keyword") String keyword,
                                        @Param("rewardTier") RewardTier rewardTier,
                                        @Param("deliveryStatus") DeliveryStatus deliveryStatus,
                                        @Param("deliveryPending") boolean deliveryPending,
                                        Pageable pageable);

    @Query(value = """
            select distinct o from FundingOrder o
            where (:campaignId is null or o.campaignId = :campaignId)
              and (:paymentStatus is null or o.paymentStatus = :paymentStatus)
              and (:rewardTier is null or o.rewardTier = :rewardTier)
              and (:ticketStatus is null or exists (select t.id from ClassTicket t where t.fundingOrder = o
                    and t.status <> com.swyp.picke.domain.ticket.enums.TicketStatus.REVOKED and t.status = :ticketStatus))
              and (:codeStatus is null or exists (select t.id from ClassTicket t where t.fundingOrder = o
                    and t.status <> com.swyp.picke.domain.ticket.enums.TicketStatus.REVOKED and t.codeStatus = :codeStatus))
              and (:deliveryPending = false or not exists (select t.id from ClassTicket t where t.fundingOrder = o
                    and t.status <> com.swyp.picke.domain.ticket.enums.TicketStatus.REVOKED)
                    or exists (select t.id from ClassTicket t where t.fundingOrder = o
                    and t.status <> com.swyp.picke.domain.ticket.enums.TicketStatus.REVOKED
                    and not exists (select a.id from TicketDeliveryAttempt a where a.ticket = t
                      and a.status = com.swyp.picke.domain.ticket.enums.DeliveryStatus.SUCCESS
                      and a.id = (select max(latest.id) from TicketDeliveryAttempt latest where latest.ticket = t))))
              and (:deliveryStatus is null
                   or (:deliveryStatus = com.swyp.picke.domain.ticket.enums.DeliveryStatus.SUCCESS
                       and exists (select t.id from ClassTicket t where t.fundingOrder = o
                         and t.status <> com.swyp.picke.domain.ticket.enums.TicketStatus.REVOKED)
                       and not exists (select t.id from ClassTicket t where t.fundingOrder = o
                         and t.status <> com.swyp.picke.domain.ticket.enums.TicketStatus.REVOKED
                         and not exists (select a.id from TicketDeliveryAttempt a where a.ticket = t
                           and a.status = com.swyp.picke.domain.ticket.enums.DeliveryStatus.SUCCESS
                           and a.id = (select max(latest.id) from TicketDeliveryAttempt latest where latest.ticket = t))))
                   or (:deliveryStatus = com.swyp.picke.domain.ticket.enums.DeliveryStatus.FAILED
                       and exists (select a.id from TicketDeliveryAttempt a where a.ticket.fundingOrder = o
                         and a.ticket.status <> com.swyp.picke.domain.ticket.enums.TicketStatus.REVOKED
                         and a.status = com.swyp.picke.domain.ticket.enums.DeliveryStatus.FAILED
                         and a.id = (select max(latest.id) from TicketDeliveryAttempt latest where latest.ticket = a.ticket))))
              and (:keyword is null
                   or lower(o.orderId) like lower(concat('%', :keyword, '%'))
                   or lower(o.supporterName) like lower(concat('%', :keyword, '%'))
                   or lower(coalesce(o.supporterEmail, '')) like lower(concat('%', :keyword, '%'))
                   or o.supporterPhone = :keyword
                   or exists (select r.id from TumblbugImportRecord r where r.fundingOrder = o
                              and lower(r.supporterNumber) like lower(concat('%', :keyword, '%'))))
            order by o.id desc
            """,
            countQuery = """
            select count(distinct o) from FundingOrder o
            where (:campaignId is null or o.campaignId = :campaignId)
              and (:paymentStatus is null or o.paymentStatus = :paymentStatus)
              and (:rewardTier is null or o.rewardTier = :rewardTier)
              and (:ticketStatus is null or exists (select t.id from ClassTicket t where t.fundingOrder = o
                    and t.status <> com.swyp.picke.domain.ticket.enums.TicketStatus.REVOKED and t.status = :ticketStatus))
              and (:codeStatus is null or exists (select t.id from ClassTicket t where t.fundingOrder = o
                    and t.status <> com.swyp.picke.domain.ticket.enums.TicketStatus.REVOKED and t.codeStatus = :codeStatus))
              and (:deliveryPending = false or not exists (select t.id from ClassTicket t where t.fundingOrder = o
                    and t.status <> com.swyp.picke.domain.ticket.enums.TicketStatus.REVOKED)
                    or exists (select t.id from ClassTicket t where t.fundingOrder = o
                    and t.status <> com.swyp.picke.domain.ticket.enums.TicketStatus.REVOKED
                    and not exists (select a.id from TicketDeliveryAttempt a where a.ticket = t
                      and a.status = com.swyp.picke.domain.ticket.enums.DeliveryStatus.SUCCESS
                      and a.id = (select max(latest.id) from TicketDeliveryAttempt latest where latest.ticket = t))))
              and (:deliveryStatus is null
                   or (:deliveryStatus = com.swyp.picke.domain.ticket.enums.DeliveryStatus.SUCCESS
                       and exists (select t.id from ClassTicket t where t.fundingOrder = o
                         and t.status <> com.swyp.picke.domain.ticket.enums.TicketStatus.REVOKED)
                       and not exists (select t.id from ClassTicket t where t.fundingOrder = o
                         and t.status <> com.swyp.picke.domain.ticket.enums.TicketStatus.REVOKED
                         and not exists (select a.id from TicketDeliveryAttempt a where a.ticket = t
                           and a.status = com.swyp.picke.domain.ticket.enums.DeliveryStatus.SUCCESS
                           and a.id = (select max(latest.id) from TicketDeliveryAttempt latest where latest.ticket = t))))
                   or (:deliveryStatus = com.swyp.picke.domain.ticket.enums.DeliveryStatus.FAILED
                       and exists (select a.id from TicketDeliveryAttempt a where a.ticket.fundingOrder = o
                         and a.ticket.status <> com.swyp.picke.domain.ticket.enums.TicketStatus.REVOKED
                         and a.status = com.swyp.picke.domain.ticket.enums.DeliveryStatus.FAILED
                         and a.id = (select max(latest.id) from TicketDeliveryAttempt latest where latest.ticket = a.ticket))))
              and (:keyword is null
                   or lower(o.orderId) like lower(concat('%', :keyword, '%'))
                   or lower(o.supporterName) like lower(concat('%', :keyword, '%'))
                   or lower(coalesce(o.supporterEmail, '')) like lower(concat('%', :keyword, '%'))
                   or o.supporterPhone = :keyword
                   or exists (select r.id from TumblbugImportRecord r where r.fundingOrder = o
                              and lower(r.supporterNumber) like lower(concat('%', :keyword, '%'))))
            """)
    Page<FundingOrder> searchOrders(@Param("paymentStatus") PaymentStatus paymentStatus,
                                    @Param("ticketStatus") TicketStatus ticketStatus,
                                    @Param("codeStatus") TicketCodeStatus codeStatus,
                                    @Param("campaignId") String campaignId,
                                    @Param("keyword") String keyword,
                                    @Param("rewardTier") RewardTier rewardTier,
                                    @Param("deliveryStatus") DeliveryStatus deliveryStatus,
                                    @Param("deliveryPending") boolean deliveryPending,
                                    Pageable pageable);
}

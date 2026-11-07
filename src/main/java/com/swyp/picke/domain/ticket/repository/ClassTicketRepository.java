package com.swyp.picke.domain.ticket.repository;

import java.time.LocalDateTime;
import com.swyp.picke.domain.ticket.enums.RegisterReminderStage;
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

public interface ClassTicketRepository extends JpaRepository<ClassTicket, Long>,
        org.springframework.data.jpa.repository.JpaSpecificationExecutor<ClassTicket> {

    String REGISTER_REMINDER_TARGET_CONDITION =
            "where t.status = com.swyp.picke.domain.ticket.enums.TicketStatus.UNREGISTERED "
            + "and t.codeStatus = com.swyp.picke.domain.ticket.enums.TicketCodeStatus.DELIVERED "
            + "and t.registerBy > :from and t.registerBy <= :to "
            + "and not exists (select 1 from TicketRegisterReminder r where r.ticket = t and r.stage = :stage "
            + "and r.status = com.swyp.picke.domain.ticket.enums.DeliveryStatus.SUCCESS)";

    /** 등록기한 알림 대상: 전달 완료·미등록이고 등록기한이 (from, to] 안이며 이 단계 알림을 아직 성공 기록하지 않은 이용권 */
    @Query("select t from ClassTicket t join fetch t.fundingOrder " + REGISTER_REMINDER_TARGET_CONDITION
            + " order by t.registerBy asc, t.id asc")
    List<ClassTicket> findRegisterReminderTargets(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to,
                                                  @Param("stage") RegisterReminderStage stage);

    @Query("select count(t) from ClassTicket t " + REGISTER_REMINDER_TARGET_CONDITION)
    long countRegisterReminderTargets(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to,
                                      @Param("stage") RegisterReminderStage stage);

    Optional<ClassTicket> findByCodeHash(String codeHash);

    boolean existsByCodeHash(String codeHash);

    /** 기존 단일 티켓 호출 호환용. 복수 코드 처리는 findAll/count 메서드를 사용합니다. */
    Optional<ClassTicket> findByFundingOrderIdAndStatusNot(Long fundingOrderId, TicketStatus status);

    List<ClassTicket> findAllByFundingOrderIdAndStatusNot(Long fundingOrderId, TicketStatus status);

    long countByFundingOrderIdAndStatusNot(Long fundingOrderId, TicketStatus status);

    @Query("select t from ClassTicket t left join fetch t.registeredUser where t.fundingOrder.id in :fundingOrderIds "
            + "and t.status <> :status order by t.id asc")
    List<ClassTicket> findByFundingOrderIdInAndStatusNotOrderByIdAsc(
            @Param("fundingOrderIds") Collection<Long> fundingOrderIds, @Param("status") TicketStatus status);

    List<ClassTicket> findByFundingOrderIdInAndStatusNot(Collection<Long> fundingOrderIds, TicketStatus status);

    long countByStatusNot(TicketStatus status);

    long countByCodeStatus(TicketCodeStatus codeStatus);

    long countByStatusIn(Collection<TicketStatus> statuses);

    /** 등록용: 같은 코드를 동시에 두 요청이 등록하지 못하도록 행 잠금(SELECT ... FOR UPDATE) */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from ClassTicket t where t.codeHash = :codeHash")
    Optional<ClassTicket> findByCodeHashForUpdate(@Param("codeHash") String codeHash);

    Optional<ClassTicket> findByRegisteredUserIdAndStatus(Long userId, TicketStatus status);

    /** 클래스 생성용: 같은 이용권으로 동시에 들어온 생성 요청을 직렬화한다 (연속 탭 중복 생성 방지) */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from ClassTicket t where t.id = :id")
    Optional<ClassTicket> findByIdForUpdate(@Param("id") Long id);

    /** 내 이용권 조회용: ACTIVE 가 있으면 그것, 없으면 가장 최근에 만료된 것 */
    Optional<ClassTicket> findTopByRegisteredUserIdAndStatusInOrderByActivatedAtDesc(
            Long userId, Collection<TicketStatus> statuses);
}

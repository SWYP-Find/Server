package com.swyp.picke.domain.ticket.repository;

import com.swyp.picke.domain.ticket.entity.FundingOrder;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FundingOrderRepository extends JpaRepository<FundingOrder, Long> {

    Optional<FundingOrder> findByOrderId(String orderId);
}

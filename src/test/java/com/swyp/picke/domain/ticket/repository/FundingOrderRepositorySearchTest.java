package com.swyp.picke.domain.ticket.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.swyp.picke.domain.ticket.entity.FundingOrder;
import com.swyp.picke.domain.ticket.enums.FundingPlatform;
import com.swyp.picke.domain.ticket.enums.PaymentStatus;
import com.swyp.picke.domain.ticket.enums.RewardTier;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
class FundingOrderRepositorySearchTest {
    @Autowired
    private FundingOrderRepository fundingOrderRepository;

    @Test
    void searchOrdersAcceptsUnfilteredAndPendingDeliveryFilters() {
        fundingOrderRepository.saveAndFlush(order("one"));
        fundingOrderRepository.saveAndFlush(order("two"));

        var all = fundingOrderRepository.searchOrders(null, null, null, null, null, null,
                false, false, false, PageRequest.of(0, 1));
        var pending = fundingOrderRepository.searchOrders(null, null, null, null, null, null,
                false, false, true, PageRequest.of(0, 20));
        var sent = fundingOrderRepository.searchOrders(null, null, null, null, null, null,
                true, false, false, PageRequest.of(0, 20));
        var failed = fundingOrderRepository.searchOrders(null, null, null, null, null, null,
                false, true, false, PageRequest.of(0, 20));

        assertThat(all.getTotalElements()).isEqualTo(2);
        assertThat(pending.getTotalElements()).isEqualTo(2);
        assertThat(sent.getTotalElements()).isZero();
        assertThat(failed.getTotalElements()).isZero();
    }

    private FundingOrder order(String suffix) {
        return FundingOrder.builder()
                .platform(FundingPlatform.WADIZ)
                .campaignId("repository-test")
                .orderId("repository-test-" + suffix)
                .rewardTier(RewardTier.SEAT_5)
                .quantity(1)
                .supporterName("test")
                .paymentStatus(PaymentStatus.PAYMENT_PENDING)
                .build();
    }
}

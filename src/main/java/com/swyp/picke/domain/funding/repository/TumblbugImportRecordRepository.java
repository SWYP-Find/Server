package com.swyp.picke.domain.funding.repository;

import com.swyp.picke.domain.funding.entity.TumblbugImportRecord;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

public interface TumblbugImportRecordRepository extends JpaRepository<TumblbugImportRecord, Long> {
    @EntityGraph(attributePaths = {"campaign", "fundingOrder"})
    List<TumblbugImportRecord> findByFundingOrderIdIn(Collection<Long> fundingOrderIds);
    boolean existsByCampaignIdAndSupporterNumberAndRewardLineId(Long campaignId, String supporterNumber, String rewardLineId);

    List<TumblbugImportRecord> findByCampaignIdAndSupporterNumberInAndRewardLineId(
            Long campaignId, Collection<String> supporterNumbers, String rewardLineId);
}

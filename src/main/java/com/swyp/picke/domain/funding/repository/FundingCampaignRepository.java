package com.swyp.picke.domain.funding.repository;

import com.swyp.picke.domain.funding.entity.FundingCampaign;
import com.swyp.picke.domain.ticket.enums.FundingPlatform;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FundingCampaignRepository extends JpaRepository<FundingCampaign, Long> {
    Optional<FundingCampaign> findByPlatformAndExternalCampaignId(FundingPlatform platform, String externalCampaignId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from FundingCampaign c where c.platform = :platform and c.externalCampaignId = :externalCampaignId")
    Optional<FundingCampaign> findByPlatformAndExternalCampaignIdForUpdate(
            @Param("platform") FundingPlatform platform,
            @Param("externalCampaignId") String externalCampaignId);
}

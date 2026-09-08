package com.swyp.picke.domain.ad.repository;

import com.swyp.picke.domain.ad.entity.AdImpressionDaily;
import com.swyp.picke.domain.ad.enums.AdSlotCode;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AdImpressionDailyRepository extends JpaRepository<AdImpressionDaily, Long> {

    Optional<AdImpressionDaily> findByCreativeIdAndSlotAndStatDate(Long creativeId, AdSlotCode slot,
                                                                   LocalDate statDate);

    /**
     * ON CONFLICT는 PostgreSQL 전용이라 테스트 H2에서 깨진다. 갱신 후 0건이면 삽입하는 방식으로 둔다.
     */
    @Modifying
    @Query("update AdImpressionDaily a set a.impressions = a.impressions + :delta "
            + "where a.creativeId = :creativeId and a.slot = :slot and a.statDate = :statDate")
    int increment(@Param("creativeId") Long creativeId,
                  @Param("slot") AdSlotCode slot,
                  @Param("statDate") LocalDate statDate,
                  @Param("delta") long delta);

    @Query("select a.creativeId as creativeId, sum(a.impressions) as total from AdImpressionDaily a "
            + "where a.statDate >= :from and a.statDate <= :to "
            + "group by a.creativeId")
    List<CreativeCount> sumByCreativeBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);

    interface CreativeCount {
        Long getCreativeId();

        long getTotal();
    }
}

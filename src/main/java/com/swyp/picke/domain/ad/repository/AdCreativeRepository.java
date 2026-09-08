package com.swyp.picke.domain.ad.repository;

import com.swyp.picke.domain.ad.entity.AdCreative;
import com.swyp.picke.domain.ad.enums.AdNetwork;
import com.swyp.picke.domain.ad.enums.AdSlotCode;
import com.swyp.picke.domain.ad.enums.AdSource;
import com.swyp.picke.domain.ad.enums.AdStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AdCreativeRepository extends JpaRepository<AdCreative, Long> {

    Optional<AdCreative> findByCode(String code);

    boolean existsByCode(String code);

    List<AdCreative> findAllBySlotAndStatus(AdSlotCode slot, AdStatus status);

    List<AdCreative> findAllByStatusOrderByIdDesc(AdStatus status);

    List<AdCreative> findAllByOrderByIdDesc();

    @Query("select c from AdCreative c "
            + "where (:network is null or c.network = :network) "
            + "and (:slot is null or c.slot = :slot) "
            + "and (:status is null or c.status = :status) "
            + "order by c.id desc")
    List<AdCreative> search(@Param("network") AdNetwork network,
                            @Param("slot") AdSlotCode slot,
                            @Param("status") AdStatus status);

    List<AdCreative> findAllByCodeIn(List<String> codes);

    List<AdCreative> findAllBySource(AdSource source);
}

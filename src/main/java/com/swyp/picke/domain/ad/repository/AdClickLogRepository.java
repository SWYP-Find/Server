package com.swyp.picke.domain.ad.repository;

import com.swyp.picke.domain.ad.entity.AdClickLog;
import com.swyp.picke.domain.admin.dto.ad.response.AdClickLogResponse;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AdClickLogRepository extends JpaRepository<AdClickLog, Long> {

    @Query("select l.creativeId as creativeId, count(l) as total from AdClickLog l "
            + "where l.createdAt >= :from and l.createdAt < :to "
            + "group by l.creativeId")
    List<CreativeCount> countByCreativeBetween(@Param("from") LocalDateTime from,
                                               @Param("to") LocalDateTime to);

    @Query("select new com.swyp.picke.domain.admin.dto.ad.response.AdClickLogResponse("
            + "l.id, c.code, c.title, c.network, l.slot, l.createdAt) "
            + "from AdClickLog l join AdCreative c on c.id = l.creativeId "
            + "where l.createdAt >= :from and l.createdAt < :to "
            + "order by l.id desc")
    Page<AdClickLogResponse> findClickLogs(@Param("from") LocalDateTime from,
                                           @Param("to") LocalDateTime to,
                                           Pageable pageable);

    interface CreativeCount {
        Long getCreativeId();

        long getTotal();
    }
}

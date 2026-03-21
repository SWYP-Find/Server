package com.swyp.app.domain.notice.repository;

import com.swyp.app.domain.notice.entity.Notice;
import com.swyp.app.domain.notice.entity.NoticePlacement;
import com.swyp.app.domain.notice.entity.NoticeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NoticeRepository extends JpaRepository<Notice, UUID> {

    @Query("SELECT notice FROM Notice notice " +
            "WHERE notice.deletedAt IS NULL " +
            "AND notice.startsAt <= :now " +
            "AND (notice.endsAt IS NULL OR notice.endsAt >= :now) " +
            "AND (:type IS NULL OR notice.type = :type) " +
            "AND (:placement IS NULL OR notice.placement = :placement) " +
            "ORDER BY notice.pinned DESC, notice.startsAt DESC, notice.createdAt DESC")
    List<Notice> findActiveNotices(@Param("now") LocalDateTime now,
                                   @Param("type") NoticeType type,
                                   @Param("placement") NoticePlacement placement,
                                   Pageable pageable);

    @Query("SELECT notice FROM Notice notice " +
            "WHERE notice.id = :noticeId " +
            "AND notice.deletedAt IS NULL " +
            "AND notice.startsAt <= :now " +
            "AND (notice.endsAt IS NULL OR notice.endsAt >= :now)")
    Optional<Notice> findActiveById(@Param("noticeId") UUID noticeId, @Param("now") LocalDateTime now);
}

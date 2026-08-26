package com.swyp.picke.domain.notification.repository;

import com.swyp.picke.domain.notification.entity.NotificationDeliveryResult;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationDeliveryResultRepository extends JpaRepository<NotificationDeliveryResult, Long> {

    Optional<NotificationDeliveryResult> findByNotificationId(Long notificationId);

    @Modifying
    @Query("""
            update NotificationDeliveryResult r
            set r.successCount = :successCount, r.failureCount = :failureCount
            where r.notificationId = :notificationId
            """)
    void updateResult(
            @Param("notificationId") Long notificationId,
            @Param("successCount") int successCount,
            @Param("failureCount") int failureCount
    );
}

package com.swyp.picke.domain.notification.repository;

import com.swyp.picke.domain.notification.entity.NotificationRead;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NotificationReadRepository extends JpaRepository<NotificationRead, Long> {

    boolean existsByNotificationIdAndUserId(Long notificationId, Long userId);

    List<NotificationRead> findByUserIdAndNotificationIdIn(Long userId, List<Long> notificationIds);

    @Modifying
    @Query(value = """
            INSERT INTO notification_reads (notification_id, user_id, created_at, updated_at)
            SELECT n.id, :userId, NOW(), NOW()
            FROM notifications n
            WHERE n.user_id IS NULL
            AND n.id NOT IN (
                SELECT nr.notification_id FROM notification_reads nr WHERE nr.user_id = :userId
            )
            """, nativeQuery = true)
    int markAllBroadcastAsRead(@Param("userId") Long userId);
}

package com.swyp.app.domain.notification.repository;

import com.swyp.app.domain.notification.entity.Notification;
import com.swyp.app.domain.notification.enums.NotificationCategory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    @Query("""
            SELECT n FROM Notification n
            WHERE (n.user.id = :userId OR n.user IS NULL)
            AND (:category IS NULL OR n.category = :category)
            ORDER BY n.createdAt DESC
            """)
    Slice<Notification> findByUserOrBroadcast(
            @Param("userId") Long userId,
            @Param("category") NotificationCategory category,
            Pageable pageable
    );

    @Modifying
    @Query("""
            UPDATE Notification n SET n.read = true, n.readAt = CURRENT_TIMESTAMP
            WHERE n.user.id = :userId AND n.read = false
            """)
    int markAllAsReadByUserId(@Param("userId") Long userId);
}

package com.swyp.picke.domain.notification.repository;

import com.swyp.picke.domain.notification.entity.Notification;
import com.swyp.picke.domain.notification.enums.NotificationCategory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    @Query("""
            SELECT n FROM Notification n
            WHERE (
                (n.user IS NOT NULL AND n.user.id = :userId)
                OR n.user IS NULL
            )
            AND (:category IS NULL OR n.category = :category)
            ORDER BY n.createdAt DESC
            """)
    Slice<Notification> findVisibleNotifications(
            @Param("userId") Long userId,
            @Param("category") NotificationCategory category,
            Pageable pageable
    );

    @Query("""
        SELECT CASE WHEN COUNT(n) > 0 THEN true ELSE false END
        FROM Notification n
        WHERE n.user IS NULL
          AND n.category = :category
          AND NOT EXISTS (
              SELECT 1 FROM NotificationRead nr
              WHERE nr.notification = n AND nr.userId = :userId
          )
        """)
    boolean hasUnreadBroadcast(@Param("userId") Long userId, @Param("category") NotificationCategory category);

    @Modifying
    @Query("""
            UPDATE Notification n SET n.read = true, n.readAt = CURRENT_TIMESTAMP
            WHERE n.user.id = :userId AND n.read = false
            """)
    int markAllAsReadByUserId(@Param("userId") Long userId);

    @Query("""
            SELECT n FROM Notification n
            WHERE (:category IS NULL OR n.category = :category)
            ORDER BY n.createdAt DESC
            """)
    Slice<Notification> findNotificationsForAdmin(
            @Param("category") NotificationCategory category,
            Pageable pageable
    );
}

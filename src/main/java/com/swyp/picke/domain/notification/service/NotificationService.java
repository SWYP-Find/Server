package com.swyp.picke.domain.notification.service;

import com.swyp.picke.domain.notification.dto.response.NotificationDetailResponse;
import com.swyp.picke.domain.notification.dto.response.NotificationListResponse;
import com.swyp.picke.domain.notification.dto.response.NotificationSummaryResponse;
import com.swyp.picke.domain.notification.entity.Notification;
import com.swyp.picke.domain.notification.entity.NotificationRead;
import com.swyp.picke.domain.notification.enums.NotificationCategory;
import com.swyp.picke.domain.notification.enums.NotificationDetailCode;
import com.swyp.picke.domain.notification.repository.NotificationReadRepository;
import com.swyp.picke.domain.notification.repository.NotificationRepository;
import com.swyp.picke.domain.user.entity.User;
import com.swyp.picke.domain.user.repository.UserRepository;
import com.swyp.picke.global.common.exception.CustomException;
import com.swyp.picke.global.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private static final int DEFAULT_PAGE_SIZE = 20;

    private final NotificationRepository notificationRepository;
    private final NotificationReadRepository notificationReadRepository;
    private final UserRepository userRepository;

    @Transactional
    public Notification createNotification(Long userId, NotificationDetailCode detailCode, String body, Long referenceId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        Notification notification = Notification.builder()
                .user(user)
                .category(detailCode.getCategory())
                .detailCode(detailCode)
                .title(detailCode.getDefaultTitle())
                .body(body)
                .referenceId(referenceId)
                .build();

        return notificationRepository.save(notification);
    }

    @Transactional
    public Notification createBroadcastNotification(NotificationDetailCode detailCode, String body, Long referenceId) {
        return createBroadcastNotification(detailCode, null, body, referenceId);
    }

    @Transactional
    public Notification createBroadcastNotification(NotificationDetailCode detailCode, String customTitle, String body, Long referenceId) {
        String resolvedTitle = (customTitle == null || customTitle.isBlank())
                ? detailCode.getDefaultTitle()
                : customTitle;
        Notification notification = Notification.builder()
                .user(null)
                .category(detailCode.getCategory())
                .detailCode(detailCode)
                .title(resolvedTitle)
                .body(body)
                .referenceId(referenceId)
                .build();

        return notificationRepository.save(notification);
    }

    public NotificationListResponse getNotifications(Long userId, NotificationCategory category, int page, int size) {
        int pageSize = size <= 0 ? DEFAULT_PAGE_SIZE : size;
        NotificationCategory filterCategory = (category == NotificationCategory.ALL) ? null : category;
        Slice<Notification> slice = notificationRepository.findVisibleNotifications(
                userId, filterCategory, PageRequest.of(page, pageSize));

        List<Long> broadcastIds = slice.getContent().stream()
                .filter(n -> n.getUser() == null)
                .map(Notification::getId)
                .toList();

        Set<Long> readBroadcastIds = broadcastIds.isEmpty()
                ? Set.of()
                : notificationReadRepository.findByUserIdAndNotificationIdIn(userId, broadcastIds)
                        .stream()
                        .map(nr -> nr.getNotification().getId())
                        .collect(Collectors.toSet());

        return new NotificationListResponse(
                slice.getContent().stream()
                        .map(n -> toSummaryResponse(n, resolveIsRead(n, readBroadcastIds)))
                        .toList(),
                slice.hasNext()
        );
    }

    public NotificationDetailResponse getNotificationDetail(Long userId, Long notificationId) {
        Notification notification = getAccessibleNotification(userId, notificationId);

        if (notification.getUser() != null) {
            return toDetailResponse(notification, notification.isRead(), notification.getReadAt());
        }

        boolean isRead = notificationReadRepository.existsByNotificationIdAndUserId(notificationId, userId);
        return toDetailResponse(notification, isRead, null);
    }

    @Transactional
    public void markAsRead(Long userId, Long notificationId) {
        Notification notification = getAccessibleNotification(userId, notificationId);

        if (notification.getUser() != null) {
            notification.markAsRead();
            return;
        }

        if (!notificationReadRepository.existsByNotificationIdAndUserId(notificationId, userId)) {
            notificationReadRepository.save(
                    NotificationRead.builder()
                            .notification(notification)
                            .userId(userId)
                            .build()
            );
        }
    }

    @Transactional
    public int markAllAsRead(Long userId) {
        int contentCount = notificationRepository.markAllAsReadByUserId(userId);
        int broadcastCount = notificationReadRepository.markAllBroadcastAsRead(userId);
        return contentCount + broadcastCount;
    }

    public boolean hasNewBroadcast(Long userId, NotificationCategory category) {
        return notificationRepository.hasUnreadBroadcast(userId, category);
    }

    private Notification getAccessibleNotification(Long userId, Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOTIFICATION_NOT_FOUND));

        boolean isAccessible = notification.getUser() == null
                || notification.getUser().getId().equals(userId);

        if (!isAccessible) {
            throw new CustomException(ErrorCode.NOTIFICATION_NOT_FOUND);
        }

        return notification;
    }

    private boolean resolveIsRead(Notification notification, Set<Long> readBroadcastIds) {
        return notification.getUser() != null
                ? notification.isRead()
                : readBroadcastIds.contains(notification.getId());
    }

    private NotificationDetailResponse toDetailResponse(Notification notification, boolean isRead, java.time.LocalDateTime readAt) {
        return new NotificationDetailResponse(
                notification.getId(),
                notification.getCategory(),
                notification.getDetailCode().name(),
                notification.getTitle(),
                notification.getBody(),
                notification.getReferenceId(),
                isRead,
                notification.getCreatedAt(),
                readAt
        );
    }

    private NotificationSummaryResponse toSummaryResponse(Notification notification, boolean isRead) {
        return new NotificationSummaryResponse(
                notification.getId(),
                notification.getCategory(),
                notification.getDetailCode().name(),
                notification.getTitle(),
                notification.getBody(),
                notification.getReferenceId(),
                isRead,
                notification.getCreatedAt()
        );
    }
}

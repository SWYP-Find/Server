package com.swyp.app.domain.notification.service;

import com.swyp.app.domain.notification.dto.response.NotificationListResponse;
import com.swyp.app.domain.notification.dto.response.NotificationSummaryResponse;
import com.swyp.app.domain.notification.entity.Notification;
import com.swyp.app.domain.notification.enums.NotificationCategory;
import com.swyp.app.domain.notification.enums.NotificationDetailCode;
import com.swyp.app.domain.notification.repository.NotificationRepository;
import com.swyp.app.domain.user.entity.User;
import com.swyp.app.domain.user.repository.UserRepository;
import com.swyp.app.global.common.exception.CustomException;
import com.swyp.app.global.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private static final int DEFAULT_PAGE_SIZE = 20;

    private final NotificationRepository notificationRepository;
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
        Notification notification = Notification.builder()
                .user(null)
                .category(detailCode.getCategory())
                .detailCode(detailCode)
                .title(detailCode.getDefaultTitle())
                .body(body)
                .referenceId(referenceId)
                .build();

        return notificationRepository.save(notification);
    }

    public NotificationListResponse getNotifications(Long userId, NotificationCategory category, int page, int size) {
        int pageSize = size <= 0 ? DEFAULT_PAGE_SIZE : size;
        NotificationCategory filterCategory = (category == NotificationCategory.ALL) ? null : category;
        Slice<Notification> slice = notificationRepository.findByUserOrBroadcast(
                userId, filterCategory, PageRequest.of(page, pageSize));

        return new NotificationListResponse(
                slice.getContent().stream().map(this::toSummaryResponse).toList(),
                slice.hasNext()
        );
    }

    @Transactional
    public void markAsRead(Long userId, Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOTIFICATION_NOT_FOUND));

        boolean isOwner = notification.getUser() != null && notification.getUser().getId().equals(userId);
        boolean isBroadcast = notification.getUser() == null;

        if (!isOwner && !isBroadcast) {
            throw new CustomException(ErrorCode.NOTIFICATION_NOT_FOUND);
        }

        notification.markAsRead();
    }

    @Transactional
    public int markAllAsRead(Long userId) {
        return notificationRepository.markAllAsReadByUserId(userId);
    }

    public boolean hasNewBroadcast(NotificationCategory category) {
        return notificationRepository.existsByUserIsNullAndCategory(category);
    }

    private NotificationSummaryResponse toSummaryResponse(Notification notification) {
        return new NotificationSummaryResponse(
                notification.getId(),
                notification.getCategory(),
                notification.getDetailCode().getCode(),
                notification.getTitle(),
                notification.getBody(),
                notification.getReferenceId(),
                notification.isRead(),
                notification.getCreatedAt()
        );
    }
}

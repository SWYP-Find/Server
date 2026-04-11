package com.swyp.picke.domain.admin.service;

import com.swyp.picke.domain.admin.dto.notification.request.AdminNoticeCreateRequest;
import com.swyp.picke.domain.admin.dto.notification.response.AdminNoticeDetailResponse;
import com.swyp.picke.domain.admin.dto.notification.response.AdminNoticeListResponse;
import com.swyp.picke.domain.admin.dto.notification.response.AdminNoticeSummaryResponse;
import com.swyp.picke.domain.notification.entity.Notification;
import com.swyp.picke.domain.notification.enums.NotificationCategory;
import com.swyp.picke.domain.notification.enums.NotificationDetailCode;
import com.swyp.picke.domain.notification.repository.NotificationRepository;
import com.swyp.picke.domain.notification.service.NotificationService;
import com.swyp.picke.global.common.exception.CustomException;
import com.swyp.picke.global.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminNotificationService {

    private static final int DEFAULT_PAGE_SIZE = 20;

    private final NotificationService notificationService;
    private final NotificationRepository notificationRepository;

    @Transactional
    public AdminNoticeDetailResponse createNotice(AdminNoticeCreateRequest request) {
        NotificationDetailCode detailCode = toDetailCode(request.category());
        Notification notification = notificationService.createBroadcastNotification(
                detailCode,
                request.title(),
                request.body(),
                null
        );
        return toDetailResponse(notification);
    }

    public AdminNoticeListResponse getNotices(NotificationCategory category, int page, int size) {
        int pageNumber = Math.max(0, page);
        int pageSize = size <= 0 ? DEFAULT_PAGE_SIZE : size;
        NotificationCategory filterCategory = normalizeCategory(category);

        Slice<Notification> slice = notificationRepository.findNotificationsForAdmin(
                filterCategory,
                PageRequest.of(pageNumber, pageSize)
        );

        return new AdminNoticeListResponse(
                slice.getContent().stream()
                        .map(this::toSummaryResponse)
                        .toList(),
                slice.hasNext()
        );
    }

    public AdminNoticeDetailResponse getNoticeDetail(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOTIFICATION_NOT_FOUND));
        return toDetailResponse(notification);
    }

    private NotificationCategory normalizeCategory(NotificationCategory category) {
        if (category == null || category == NotificationCategory.ALL) {
            return null;
        }
        return category;
    }

    private NotificationDetailCode toDetailCode(NotificationCategory category) {
        if (category == NotificationCategory.CONTENT) {
            return NotificationDetailCode.NEW_BATTLE;
        }
        if (category == NotificationCategory.NOTICE) {
            return NotificationDetailCode.POLICY_CHANGE;
        }
        if (category == NotificationCategory.EVENT) {
            return NotificationDetailCode.PROMOTION;
        }
        throw new CustomException(ErrorCode.BAD_REQUEST);
    }

    private AdminNoticeSummaryResponse toSummaryResponse(Notification notification) {
        return new AdminNoticeSummaryResponse(
                notification.getId(),
                notification.getCategory(),
                notification.getDetailCode().name(),
                notification.getTitle(),
                notification.getBody(),
                notification.getReferenceId(),
                notification.getCreatedAt()
        );
    }

    private AdminNoticeDetailResponse toDetailResponse(Notification notification) {
        return new AdminNoticeDetailResponse(
                notification.getId(),
                notification.getCategory(),
                notification.getDetailCode().name(),
                notification.getTitle(),
                notification.getBody(),
                notification.getReferenceId(),
                notification.getCreatedAt()
        );
    }
}

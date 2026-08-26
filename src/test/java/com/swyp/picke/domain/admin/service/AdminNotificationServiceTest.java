package com.swyp.picke.domain.admin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.swyp.picke.domain.admin.dto.notification.request.AdminNoticeUpdateRequest;
import com.swyp.picke.domain.admin.dto.notification.response.AdminNoticeDetailResponse;
import com.swyp.picke.domain.notification.entity.Notification;
import com.swyp.picke.domain.notification.entity.NotificationDeliveryResult;
import com.swyp.picke.domain.notification.enums.NotificationCategory;
import com.swyp.picke.domain.notification.enums.NotificationDetailCode;
import com.swyp.picke.domain.notification.repository.NotificationDeliveryResultRepository;
import com.swyp.picke.domain.notification.repository.NotificationRepository;
import com.swyp.picke.domain.notification.service.NotificationDispatchService;
import com.swyp.picke.domain.notification.service.NotificationService;
import com.swyp.picke.global.common.exception.CustomException;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminNotificationServiceTest {

    @Mock
    private NotificationService notificationService;

    @Mock
    private NotificationDispatchService notificationDispatchService;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationDeliveryResultRepository notificationDeliveryResultRepository;

    @InjectMocks
    private AdminNotificationService adminNotificationService;

    private Notification newNotice(String title, String body) {
        return Notification.builder()
                .user(null)
                .category(NotificationCategory.NOTICE)
                .detailCode(NotificationDetailCode.POLICY_CHANGE)
                .title(title)
                .body(body)
                .build();
    }

    @Test
    @DisplayName("공지사항을 수정한다")
    void updateNotice_updatesExistingNotice() {
        Notification notification = newNotice("원본 제목", "원본 본문");
        when(notificationRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(notification));

        AdminNoticeDetailResponse response = adminNotificationService.updateNotice(
                1L, new AdminNoticeUpdateRequest("수정된 제목", "수정된 본문"));

        assertThat(response.title()).isEqualTo("수정된 제목");
        assertThat(response.body()).isEqualTo("수정된 본문");
    }

    @Test
    @DisplayName("존재하지 않는 공지사항을 수정하면 예외를 던진다")
    void updateNotice_throws_whenNotFound() {
        when(notificationRepository.findByIdAndDeletedAtIsNull(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminNotificationService.updateNotice(
                999L, new AdminNoticeUpdateRequest("제목", "본문")))
                .isInstanceOf(CustomException.class);
    }

    @Test
    @DisplayName("공지사항을 삭제하면 deletedAt이 기록된다")
    void deleteNotice_softDeletesExistingNotice() {
        Notification notification = newNotice("제목", "본문");
        when(notificationRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(notification));

        adminNotificationService.deleteNotice(1L);

        assertThat(notification.getDeletedAt()).isNotNull();
    }

    @Test
    @DisplayName("존재하지 않는 공지사항을 삭제하면 예외를 던진다")
    void deleteNotice_throws_whenNotFound() {
        when(notificationRepository.findByIdAndDeletedAtIsNull(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminNotificationService.deleteNotice(999L))
                .isInstanceOf(CustomException.class);
    }

    @Test
    @DisplayName("공지사항 발송 결과를 조회한다")
    void getDeliveryResult_returnsResult() {
        Notification notification = newNotice("제목", "본문");
        when(notificationRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(notification));
        NotificationDeliveryResult deliveryResult = NotificationDeliveryResult.builder()
                .notificationId(1L)
                .targetCount(10)
                .build();
        when(notificationDeliveryResultRepository.findByNotificationId(1L))
                .thenReturn(Optional.of(deliveryResult));

        var response = adminNotificationService.getDeliveryResult(1L);

        assertThat(response.notificationId()).isEqualTo(1L);
        assertThat(response.targetCount()).isEqualTo(10);
        assertThat(response.successCount()).isEqualTo(0);
        assertThat(response.failureCount()).isEqualTo(0);
        assertThat(response.pending()).isTrue();
    }

    @Test
    @DisplayName("존재하지 않는 공지사항의 발송 결과를 조회하면 예외를 던진다")
    void getDeliveryResult_throws_whenNoticeNotFound() {
        when(notificationRepository.findByIdAndDeletedAtIsNull(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminNotificationService.getDeliveryResult(999L))
                .isInstanceOf(CustomException.class);
    }

    @Test
    @DisplayName("발송 결과가 아직 집계되지 않은 공지사항을 조회하면 예외를 던진다")
    void getDeliveryResult_throws_whenResultNotFound() {
        Notification notification = newNotice("제목", "본문");
        when(notificationRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(notification));
        when(notificationDeliveryResultRepository.findByNotificationId(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminNotificationService.getDeliveryResult(1L))
                .isInstanceOf(CustomException.class);
    }
}

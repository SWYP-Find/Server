package com.swyp.app.domain.notification.service;

import com.swyp.app.domain.notification.dto.response.NotificationListResponse;
import com.swyp.app.domain.notification.entity.Notification;
import com.swyp.app.domain.notification.enums.NotificationCategory;
import com.swyp.app.domain.notification.enums.NotificationDetailCode;
import com.swyp.app.domain.notification.repository.NotificationRepository;
import com.swyp.app.domain.user.entity.User;
import com.swyp.app.domain.user.repository.UserRepository;
import com.swyp.app.global.common.exception.CustomException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.SliceImpl;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private NotificationService notificationService;

    @Test
    @DisplayName("개인 알림을 생성한다")
    void createNotification_creates_personal_notification() {
        Long userId = 1L;
        User user = createMockUser();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(i -> i.getArgument(0));

        Notification result = notificationService.createNotification(
                userId, NotificationDetailCode.NEW_BATTLE, "새 배틀이 시작되었습니다", 100L);

        assertThat(result.getCategory()).isEqualTo(NotificationCategory.CONTENT);
        assertThat(result.getDetailCode()).isEqualTo(NotificationDetailCode.NEW_BATTLE);
        assertThat(result.getBody()).isEqualTo("새 배틀이 시작되었습니다");
        assertThat(result.getReferenceId()).isEqualTo(100L);
    }

    @Test
    @DisplayName("브로드캐스트 알림을 생성한다")
    void createBroadcastNotification_creates_with_null_user() {
        when(notificationRepository.save(any(Notification.class))).thenAnswer(i -> i.getArgument(0));

        Notification result = notificationService.createBroadcastNotification(
                NotificationDetailCode.POLICY_CHANGE, "서비스 정책이 변경되었습니다", 50L);

        assertThat(result.getUser()).isNull();
        assertThat(result.getCategory()).isEqualTo(NotificationCategory.NOTICE);
        assertThat(result.getDetailCode()).isEqualTo(NotificationDetailCode.POLICY_CHANGE);
    }

    @Test
    @DisplayName("알림 목록을 카테고리별로 조회한다")
    void getNotifications_returns_filtered_list() {
        Long userId = 1L;
        Notification notification = Notification.builder()
                .user(null)
                .category(NotificationCategory.CONTENT)
                .detailCode(NotificationDetailCode.NEW_BATTLE)
                .title("새로운 배틀이 시작되었어요")
                .body("배틀 내용")
                .referenceId(1L)
                .build();

        when(notificationRepository.findByUserOrBroadcast(eq(userId), eq(NotificationCategory.CONTENT), any(Pageable.class)))
                .thenReturn(new SliceImpl<>(List.of(notification)));

        NotificationListResponse response = notificationService.getNotifications(userId, NotificationCategory.CONTENT, 0, 20);

        assertThat(response.items()).hasSize(1);
        assertThat(response.items().getFirst().category()).isEqualTo(NotificationCategory.CONTENT);
        assertThat(response.items().getFirst().detailCode()).isEqualTo(1);
        assertThat(response.hasNext()).isFalse();
    }

    @Test
    @DisplayName("존재하지 않는 알림 읽음 처리 시 예외를 던진다")
    void markAsRead_throws_when_not_found() {
        when(notificationRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.markAsRead(1L, 999L))
                .isInstanceOf(CustomException.class);
    }

    @Test
    @DisplayName("전체 읽음 처리를 실행한다")
    void markAllAsRead_calls_repository() {
        Long userId = 1L;
        when(notificationRepository.markAllAsReadByUserId(userId)).thenReturn(5);

        int count = notificationService.markAllAsRead(userId);

        assertThat(count).isEqualTo(5);
        verify(notificationRepository).markAllAsReadByUserId(userId);
    }

    private User createMockUser() {
        return User.builder()
                .userTag("test-user-tag")
                .nickname("테스트유저")
                .build();
    }
}

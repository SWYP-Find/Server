package com.swyp.picke.domain.notification.service;

import com.swyp.picke.domain.notification.dto.response.NotificationDetailResponse;
import com.swyp.picke.domain.notification.dto.response.NotificationListResponse;
import com.swyp.picke.domain.notification.entity.Notification;
import com.swyp.picke.domain.notification.entity.NotificationRead;
import com.swyp.picke.domain.notification.enums.NotificationCategory;
import com.swyp.picke.domain.notification.enums.NotificationDetailCode;
import com.swyp.picke.domain.notification.repository.NotificationReadRepository;
import com.swyp.picke.domain.notification.repository.NotificationRepository;
import com.swyp.picke.domain.user.entity.User;
import com.swyp.picke.domain.user.repository.UserRepository;
import com.swyp.picke.global.common.exception.CustomException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
    private NotificationReadRepository notificationReadRepository;

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
        User user = createMockUser();
        Notification notification = Notification.builder()
                .user(user)
                .category(NotificationCategory.CONTENT)
                .detailCode(NotificationDetailCode.NEW_BATTLE)
                .title("새로운 배틀이 시작되었어요")
                .body("배틀 내용")
                .referenceId(1L)
                .build();

        setUserId(user, userId);

        when(notificationRepository.findVisibleNotifications(eq(userId), eq(NotificationCategory.CONTENT), any(Pageable.class)))
                .thenReturn(new SliceImpl<>(List.of(notification)));

        NotificationListResponse response = notificationService.getNotifications(userId, NotificationCategory.CONTENT, 0, 20);

        assertThat(response.items()).hasSize(1);
        assertThat(response.items().getFirst().category()).isEqualTo(NotificationCategory.CONTENT);
        assertThat(response.items().getFirst().detailCode()).isEqualTo(1);
        assertThat(response.hasNext()).isFalse();
    }

    @Test
    @DisplayName("브로드캐스트 알림 목록 조회 시 사용자별 읽음 상태를 반영한다")
    void getNotifications_resolves_broadcast_read_status() {
        Long userId = 1L;
        Notification broadcastNotification = Notification.builder()
                .user(null)
                .category(NotificationCategory.NOTICE)
                .detailCode(NotificationDetailCode.POLICY_CHANGE)
                .title("공지사항")
                .body("서비스 정책이 변경되었습니다")
                .referenceId(50L)
                .build();

        setNotificationId(broadcastNotification, 20L);

        when(notificationRepository.findVisibleNotifications(eq(userId), eq(NotificationCategory.NOTICE), any(Pageable.class)))
                .thenReturn(new SliceImpl<>(List.of(broadcastNotification)));

        NotificationRead readRecord = NotificationRead.builder()
                .notification(broadcastNotification)
                .userId(userId)
                .build();

        when(notificationReadRepository.findByUserIdAndNotificationIdIn(userId, List.of(20L)))
                .thenReturn(List.of(readRecord));

        NotificationListResponse response = notificationService.getNotifications(userId, NotificationCategory.NOTICE, 0, 20);

        assertThat(response.items()).hasSize(1);
        assertThat(response.items().getFirst().isRead()).isTrue();
    }

    @Test
    @DisplayName("존재하지 않는 알림 읽음 처리 시 예외를 던진다")
    void markAsRead_throws_when_not_found() {
        when(notificationRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.markAsRead(1L, 999L))
                .isInstanceOf(CustomException.class);
    }

    @Test
    @DisplayName("전역 알림 읽음 처리 시 NotificationRead 레코드를 저장한다")
    void markAsRead_saves_notification_read_for_broadcast() {
        Long userId = 1L;
        Long notificationId = 20L;
        Notification notification = Notification.builder()
                .user(null)
                .category(NotificationCategory.NOTICE)
                .detailCode(NotificationDetailCode.POLICY_CHANGE)
                .title("공지사항")
                .body("서비스 정책이 변경되었습니다")
                .referenceId(50L)
                .build();

        when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(notification));
        when(notificationReadRepository.existsByNotificationIdAndUserId(notificationId, userId)).thenReturn(false);

        notificationService.markAsRead(userId, notificationId);

        verify(notificationReadRepository).save(any(NotificationRead.class));
    }

    @Test
    @DisplayName("본인 알림 상세를 조회한다")
    void getNotificationDetail_returns_owned_notification() {
        Long userId = 1L;
        User user = createMockUser();
        Notification notification = Notification.builder()
                .user(user)
                .category(NotificationCategory.CONTENT)
                .detailCode(NotificationDetailCode.NEW_BATTLE)
                .title("새로운 배틀이 시작되었어요")
                .body("배틀 내용")
                .referenceId(1L)
                .build();

        setUserId(user, userId);
        setNotificationId(notification, 10L);

        when(notificationRepository.findById(10L)).thenReturn(Optional.of(notification));

        NotificationDetailResponse response = notificationService.getNotificationDetail(userId, 10L);

        assertThat(response.notificationId()).isEqualTo(10L);
        assertThat(response.category()).isEqualTo(NotificationCategory.CONTENT);
        assertThat(response.detailCode()).isEqualTo(1);
        assertThat(response.title()).isEqualTo("새로운 배틀이 시작되었어요");
    }

    @Test
    @DisplayName("브로드캐스트 알림 상세를 조회한다")
    void getNotificationDetail_returns_broadcast_notification() {
        Long userId = 1L;
        Long notificationId = 20L;
        Notification notification = Notification.builder()
                .user(null)
                .category(NotificationCategory.NOTICE)
                .detailCode(NotificationDetailCode.POLICY_CHANGE)
                .title("공지사항")
                .body("서비스 정책이 변경되었습니다")
                .referenceId(50L)
                .build();

        setNotificationId(notification, notificationId);

        when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(notification));
        when(notificationReadRepository.existsByNotificationIdAndUserId(notificationId, userId)).thenReturn(false);

        NotificationDetailResponse response = notificationService.getNotificationDetail(userId, notificationId);

        assertThat(response.notificationId()).isEqualTo(20L);
        assertThat(response.category()).isEqualTo(NotificationCategory.NOTICE);
        assertThat(response.detailCode()).isEqualTo(4);
        assertThat(response.body()).isEqualTo("서비스 정책이 변경되었습니다");
        assertThat(response.isRead()).isFalse();
    }

    @Test
    @DisplayName("다른 사용자의 알림 상세 조회 시 예외를 던진다")
    void getNotificationDetail_throws_when_notification_not_accessible() {
        Long ownerId = 1L;
        Long requesterId = 2L;
        User owner = createMockUser();
        Notification notification = Notification.builder()
                .user(owner)
                .category(NotificationCategory.CONTENT)
                .detailCode(NotificationDetailCode.NEW_BATTLE)
                .title("새로운 배틀이 시작되었어요")
                .body("배틀 내용")
                .referenceId(1L)
                .build();

        setUserId(owner, ownerId);
        when(notificationRepository.findById(30L)).thenReturn(Optional.of(notification));

        assertThatThrownBy(() -> notificationService.getNotificationDetail(requesterId, 30L))
                .isInstanceOf(CustomException.class);
    }

    @Test
    @DisplayName("전체 읽음 처리를 실행한다")
    void markAllAsRead_calls_repository() {
        Long userId = 1L;
        when(notificationRepository.markAllAsReadByUserId(userId)).thenReturn(5);
        when(notificationReadRepository.markAllBroadcastAsRead(userId)).thenReturn(3);

        int count = notificationService.markAllAsRead(userId);

        assertThat(count).isEqualTo(8);
        verify(notificationRepository).markAllAsReadByUserId(userId);
        verify(notificationReadRepository).markAllBroadcastAsRead(userId);
    }

    private User createMockUser() {
        return User.builder()
                .userTag("test-user-tag")
                .nickname("테스트유저")
                .build();
    }

    private void setUserId(User user, Long id) {
        try {
            var field = User.class.getSuperclass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(user, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private void setNotificationId(Notification notification, Long id) {
        try {
            var field = Notification.class.getSuperclass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(notification, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}

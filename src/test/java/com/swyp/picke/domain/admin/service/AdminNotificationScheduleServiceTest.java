package com.swyp.picke.domain.admin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.swyp.picke.domain.admin.dto.notification.request.AdminNotificationScheduleRequest;
import com.swyp.picke.domain.admin.dto.notification.request.AdminNotificationScheduleTestRequest;
import com.swyp.picke.domain.admin.dto.notification.request.AdminNotificationScheduleToggleRequest;
import com.swyp.picke.domain.admin.dto.notification.response.AdminNotificationScheduleListResponse;
import com.swyp.picke.domain.admin.dto.notification.response.AdminNotificationScheduleResponse;
import com.swyp.picke.domain.notification.entity.NotificationSchedule;
import com.swyp.picke.domain.notification.repository.NotificationScheduleRepository;
import com.swyp.picke.domain.notification.service.NotificationDispatchService;
import com.swyp.picke.global.common.exception.CustomException;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminNotificationScheduleServiceTest {

    @Mock
    private NotificationScheduleRepository notificationScheduleRepository;

    @Mock
    private NotificationDispatchService notificationDispatchService;

    @InjectMocks
    private AdminNotificationScheduleService adminNotificationScheduleService;

    @Test
    @DisplayName("예약 알림을 생성한다")
    void create_savesSchedule() {
        AdminNotificationScheduleRequest request =
                new AdminNotificationScheduleRequest("오늘의 질문", "지금 확인해보세요", LocalTime.of(19, 0), true);
        when(notificationScheduleRepository.save(any(NotificationSchedule.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AdminNotificationScheduleResponse response = adminNotificationScheduleService.create(request);

        assertThat(response.title()).isEqualTo("오늘의 질문");
        assertThat(response.subtitle()).isEqualTo("지금 확인해보세요");
        assertThat(response.sendTime()).isEqualTo(LocalTime.of(19, 0));
        assertThat(response.enabled()).isTrue();
    }

    @Test
    @DisplayName("예약 알림 목록을 조회한다")
    void getSchedules_returnsAllSchedules() {
        NotificationSchedule schedule = NotificationSchedule.builder()
                .title("오늘의 질문")
                .subtitle("지금 확인해보세요")
                .sendTime(LocalTime.of(19, 0))
                .enabled(true)
                .build();
        when(notificationScheduleRepository.findAll()).thenReturn(List.of(schedule));

        AdminNotificationScheduleListResponse response = adminNotificationScheduleService.getSchedules();

        assertThat(response.schedules()).hasSize(1);
        assertThat(response.schedules().getFirst().title()).isEqualTo("오늘의 질문");
    }

    @Test
    @DisplayName("존재하지 않는 예약 알림을 조회하면 예외를 던진다")
    void getSchedule_throws_whenNotFound() {
        when(notificationScheduleRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminNotificationScheduleService.getSchedule(999L))
                .isInstanceOf(CustomException.class);
    }

    @Test
    @DisplayName("예약 알림을 수정한다")
    void update_updatesExistingSchedule() {
        NotificationSchedule schedule = NotificationSchedule.builder()
                .title("오늘의 질문")
                .subtitle("지금 확인해보세요")
                .sendTime(LocalTime.of(19, 0))
                .enabled(true)
                .build();
        when(notificationScheduleRepository.findById(1L)).thenReturn(Optional.of(schedule));

        AdminNotificationScheduleRequest request =
                new AdminNotificationScheduleRequest("수정된 제목", "수정된 소제목", LocalTime.of(20, 30), false);

        AdminNotificationScheduleResponse response = adminNotificationScheduleService.update(1L, request);

        assertThat(response.title()).isEqualTo("수정된 제목");
        assertThat(response.subtitle()).isEqualTo("수정된 소제목");
        assertThat(response.sendTime()).isEqualTo(LocalTime.of(20, 30));
        assertThat(response.enabled()).isFalse();
    }

    @Test
    @DisplayName("존재하지 않는 예약 알림을 수정하면 예외를 던진다")
    void update_throws_whenNotFound() {
        when(notificationScheduleRepository.findById(999L)).thenReturn(Optional.empty());
        AdminNotificationScheduleRequest request =
                new AdminNotificationScheduleRequest("제목", "소제목", LocalTime.of(19, 0), true);

        assertThatThrownBy(() -> adminNotificationScheduleService.update(999L, request))
                .isInstanceOf(CustomException.class);
    }

    @Test
    @DisplayName("예약 알림의 On/Off 상태를 전환한다")
    void toggle_updatesEnabledState() {
        NotificationSchedule schedule = NotificationSchedule.builder()
                .title("오늘의 질문")
                .subtitle("지금 확인해보세요")
                .sendTime(LocalTime.of(19, 0))
                .enabled(true)
                .build();
        when(notificationScheduleRepository.findById(1L)).thenReturn(Optional.of(schedule));

        AdminNotificationScheduleResponse response =
                adminNotificationScheduleService.toggle(1L, new AdminNotificationScheduleToggleRequest(false));

        assertThat(response.enabled()).isFalse();
    }

    @Test
    @DisplayName("존재하지 않는 예약 알림을 전환하면 예외를 던진다")
    void toggle_throws_whenNotFound() {
        when(notificationScheduleRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminNotificationScheduleService.toggle(999L, new AdminNotificationScheduleToggleRequest(true)))
                .isInstanceOf(CustomException.class);
    }

    @Test
    @DisplayName("예약 알림 저장 내용으로 특정 유저에게 테스트 발송한다")
    void sendTest_dispatchesSavedScheduleContent() {
        NotificationSchedule schedule = NotificationSchedule.builder()
                .title("오늘의 질문")
                .subtitle("지금 확인해보세요")
                .sendTime(LocalTime.of(19, 0))
                .enabled(true)
                .build();
        when(notificationScheduleRepository.findById(1L)).thenReturn(Optional.of(schedule));

        adminNotificationScheduleService.sendTest(1L, new AdminNotificationScheduleTestRequest(123L));

        verify(notificationDispatchService).sendTestPush(123L, "오늘의 질문", "지금 확인해보세요");
    }

    @Test
    @DisplayName("존재하지 않는 예약 알림을 테스트 발송하면 예외를 던진다")
    void sendTest_throws_whenNotFound() {
        when(notificationScheduleRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminNotificationScheduleService.sendTest(
                999L, new AdminNotificationScheduleTestRequest(123L)))
                .isInstanceOf(CustomException.class);
    }

    @Test
    @DisplayName("예약 알림을 삭제한다")
    void delete_removesExistingSchedule() {
        NotificationSchedule schedule = NotificationSchedule.builder()
                .title("오늘의 질문")
                .subtitle("지금 확인해보세요")
                .sendTime(LocalTime.of(19, 0))
                .enabled(true)
                .build();
        when(notificationScheduleRepository.findById(1L)).thenReturn(Optional.of(schedule));

        adminNotificationScheduleService.delete(1L);

        verify(notificationScheduleRepository).delete(schedule);
    }

    @Test
    @DisplayName("존재하지 않는 예약 알림을 삭제하면 예외를 던진다")
    void delete_throws_whenNotFound() {
        when(notificationScheduleRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminNotificationScheduleService.delete(999L))
                .isInstanceOf(CustomException.class);
    }
}

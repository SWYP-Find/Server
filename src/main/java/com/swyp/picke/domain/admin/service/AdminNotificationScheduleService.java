package com.swyp.picke.domain.admin.service;

import com.swyp.picke.domain.admin.dto.notification.request.AdminNotificationScheduleRequest;
import com.swyp.picke.domain.admin.dto.notification.response.AdminNotificationScheduleListResponse;
import com.swyp.picke.domain.admin.dto.notification.response.AdminNotificationScheduleResponse;
import com.swyp.picke.domain.notification.entity.NotificationSchedule;
import com.swyp.picke.domain.notification.repository.NotificationScheduleRepository;
import com.swyp.picke.global.common.exception.CustomException;
import com.swyp.picke.global.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminNotificationScheduleService {

    private final NotificationScheduleRepository notificationScheduleRepository;

    @Transactional
    public AdminNotificationScheduleResponse create(AdminNotificationScheduleRequest request) {
        NotificationSchedule schedule = NotificationSchedule.builder()
                .title(request.title())
                .subtitle(request.subtitle())
                .sendTime(request.sendTime())
                .enabled(request.enabled())
                .build();

        return toResponse(notificationScheduleRepository.save(schedule));
    }

    public AdminNotificationScheduleListResponse getSchedules() {
        return new AdminNotificationScheduleListResponse(
                notificationScheduleRepository.findAll().stream()
                        .map(this::toResponse)
                        .toList()
        );
    }

    public AdminNotificationScheduleResponse getSchedule(Long scheduleId) {
        return toResponse(getExistingSchedule(scheduleId));
    }

    @Transactional
    public AdminNotificationScheduleResponse update(Long scheduleId, AdminNotificationScheduleRequest request) {
        NotificationSchedule schedule = getExistingSchedule(scheduleId);
        schedule.update(request.title(), request.subtitle(), request.sendTime(), request.enabled());
        return toResponse(schedule);
    }

    @Transactional
    public void delete(Long scheduleId) {
        NotificationSchedule schedule = getExistingSchedule(scheduleId);
        notificationScheduleRepository.delete(schedule);
    }

    private NotificationSchedule getExistingSchedule(Long scheduleId) {
        return notificationScheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOTIFICATION_SCHEDULE_NOT_FOUND));
    }

    private AdminNotificationScheduleResponse toResponse(NotificationSchedule schedule) {
        return new AdminNotificationScheduleResponse(
                schedule.getId(),
                schedule.getTitle(),
                schedule.getSubtitle(),
                schedule.getSendTime(),
                schedule.isEnabled(),
                schedule.getCreatedAt()
        );
    }
}

package com.swyp.picke.domain.admin.dto.notification.response;

import java.util.List;

public record AdminNotificationScheduleListResponse(
        List<AdminNotificationScheduleResponse> schedules
) {}

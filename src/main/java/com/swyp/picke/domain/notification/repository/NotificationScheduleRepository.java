package com.swyp.picke.domain.notification.repository;

import com.swyp.picke.domain.notification.entity.NotificationSchedule;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationScheduleRepository extends JpaRepository<NotificationSchedule, Long> {

    List<NotificationSchedule> findAllByEnabledTrue();
}

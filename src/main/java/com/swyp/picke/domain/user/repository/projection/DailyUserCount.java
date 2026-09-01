package com.swyp.picke.domain.user.repository.projection;

import java.time.LocalDate;

public interface DailyUserCount {
    LocalDate getActivityDate();
    long getCount();
}

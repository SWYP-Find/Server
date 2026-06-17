package com.swyp.picke.domain.attendance.enums;

public enum AttendanceStatus {
    ATTENDED,       // 해당 요일 출석 완료
    NOT_ATTENDED,   // 해당 요일 미출석 (오늘 이후 포함)
    PENDING         // 오늘 날짜이며 아직 출석 전
}

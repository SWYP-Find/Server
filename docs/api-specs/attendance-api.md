# 출석체크 API 명세서

## 1. 설계 메모

- 출석체크 API는 `snake_case` 필드명을 기준으로 합니다.
- 출석 주기는 **월요일 기준 1주일(월~일)**로 초기화됩니다.
- 매일 1회 출석 체크 시 `+5P`를 지급하고, 7일 연속 출석 달성 시 추가로 `+7P`를 지급합니다.
- 7일 연속 출석 실패 시 다음 주 월요일에 연속 출석 카운트가 초기화됩니다.
- 인증이 필요한 모든 API는 `Authorization: Bearer {access_token}` 헤더를 요구합니다.
- 외부 응답에서는 내부 PK인 `user_id`를 노출하지 않고 `user_tag`를 사용합니다.
- 포인트 단위는 `P(포인트)`이며, 지급 내역은 별도 이력 테이블에 저장합니다.

### 1.1 출석 상태 정의

| 상태 값 | 설명 |
|---------|------|
| `ATTENDED` | 해당 요일 출석 완료 |
| `NOT_ATTENDED` | 해당 요일 미출석 (오늘 이후 포함) |
| `PENDING` | 오늘 날짜이며 아직 출석 전 |

### 1.2 출석 보상 정책

| 조건 | 지급 포인트 |
|------|-------------|
| 매일 출석 체크 | `+5P` |
| 7일 연속 출석 달성 (일요일) | 추가 `+7P` |

### 1.3 연속 출석 초기화 정책

- 연속 출석 카운트는 **매주 월요일 00:00**에 초기화됩니다.
- 한 주(월~일) 내에 출석하지 않은 날이 있어도, 해당 주 일요일까지는 연속 출석 달성이 가능합니다.
- 단, 전날 출석 없이 오늘 출석 시 연속 카운트는 1로 리셋됩니다.

---

## 2. 출석체크 API

### 2.1 `POST /api/v1/attendance/check`

오늘의 출석을 체크하고 포인트를 지급합니다.

- 하루에 1회만 출석 가능합니다. 중복 출석 시 `409`를 반환합니다.
- 출석 성공 시 `+5P`를 즉시 지급합니다.
- 7일 연속 출석 달성 시 `+5P`에 추가로 `+7P`를 지급합니다.

요청 헤더:

- `Authorization: Bearer {access_token}`

응답:

```json
{
  "statusCode": 200,
  "data": {
    "user_tag": "pique-5010f67e",
    "attended_at": "2025-07-10T09:00:00Z",
    "points_earned": 5,
    "streak_bonus_earned": false,
    "streak_bonus_points": 0,
    "consecutive_days": 4,
    "total_points": 125
  },
  "error": null
}
```

7일 연속 출석 달성 시 응답:

```json
{
  "statusCode": 200,
  "data": {
    "user_tag": "pique-5010f67e",
    "attended_at": "2025-07-13T09:00:00Z",
    "points_earned": 5,
    "streak_bonus_earned": true,
    "streak_bonus_points": 7,
    "consecutive_days": 7,
    "total_points": 167
  },
  "error": null
}
```

---

### 2.2 `GET /api/v1/attendance/weekly`

이번 주(월~일) 출석 현황 및 연속 출석 정보를 조회합니다.

- 요일별 출석 상태와 현재 연속 출석 일수를 반환합니다.
- 현재 주의 월요일 기준으로 데이터를 반환합니다.

요청 헤더:

- `Authorization: Bearer {access_token}`

응답:

```json
{
  "statusCode": 200,
  "data": {
    "user_tag": "pique-5010f67e",
    "week_start_date": "2025-07-07",
    "consecutive_days": 4,
    "is_streak_achieved": false,
    "weekly_attendance": [
      { "day": "MON", "date": "2025-07-07", "status": "ATTENDED", "points": 5 },
      { "day": "TUE", "date": "2025-07-08", "status": "ATTENDED", "points": 5 },
      { "day": "WED", "date": "2025-07-09", "status": "ATTENDED", "points": 5 },
      { "day": "THU", "date": "2025-07-10", "status": "ATTENDED", "points": 5 },
      { "day": "FRI", "date": "2025-07-11", "status": "PENDING",   "points": 0 },
      { "day": "SAT", "date": "2025-07-12", "status": "NOT_ATTENDED", "points": 0 },
      { "day": "SUN", "date": "2025-07-13", "status": "NOT_ATTENDED", "points": 0 }
    ],
    "streak_reward_points": 7
  },
  "error": null
}
```

---

### 2.3 `GET /api/v1/attendance/summary`

사용자의 전체 출석 통계 및 누적 포인트를 조회합니다.

- 총 출석 일수, 최장 연속 출석 일수, 현재 연속 출석 일수, 총 획득 포인트를 반환합니다.

요청 헤더:

- `Authorization: Bearer {access_token}`

응답:

```json
{
  "statusCode": 200,
  "data": {
    "user_tag": "pique-5010f67e",
    "total_attended_days": 32,
    "current_consecutive_days": 4,
    "max_consecutive_days": 14,
    "total_points_from_attendance": 185,
    "last_attended_at": "2025-07-10T09:00:00Z"
  },
  "error": null
}
```

---

### 2.4 `GET /api/v1/attendance/points/history`

출석 포인트 지급 이력을 조회합니다.

- 최신순으로 반환하며, 페이지네이션을 지원합니다.

요청 헤더:

- `Authorization: Bearer {access_token}`

쿼리 파라미터:

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| `page` | `integer` | N | 페이지 번호 (기본값: `1`) |
| `size` | `integer` | N | 페이지 크기 (기본값: `20`, 최대: `50`) |

응답:

```json
{
  "statusCode": 200,
  "data": {
    "total_count": 32,
    "page": 1,
    "size": 20,
    "items": [
      {
        "rewarded_at": "2025-07-10T09:00:00Z",
        "points": 5,
        "reason": "DAILY_ATTENDANCE",
        "consecutive_days": 4
      },
      {
        "rewarded_at": "2025-07-06T08:30:00Z",
        "points": 7,
        "reason": "STREAK_BONUS",
        "consecutive_days": 7
      }
    ]
  },
  "error": null
}
```

가능한 `reason` 값:

- `DAILY_ATTENDANCE` — 일일 출석 보상 (`+5P`)
- `STREAK_BONUS` — 7일 연속 출석 달성 보상 (`+7P`)

---

## 3. 어드민 API

- 어드민 전용 API는 `Authorization: Bearer {access_token}` 헤더와 함께 어드민 권한이 있는 계정만 호출 가능합니다.
- 어드민 권한이 없는 계정이 호출 시 `403`과 `FORBIDDEN`을 반환합니다.

### 3.1 `GET /api/v1/admin/attendance/stats`

전체 유저의 출석 현황을 집계하여 반환합니다.

- 일별 출석자 수, 전체 출석률 등 서비스 운영 지표를 제공합니다.
- `from` ~ `to` 범위를 지정하지 않으면 최근 7일 기준으로 반환합니다.
- `from` ~ `to` 최대 조회 범위는 90일입니다.

요청 헤더:

- `Authorization: Bearer {access_token}`

쿼리 파라미터:

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| `from` | `string` | N | 조회 시작일 (`YYYY-MM-DD`, 기본값: 7일 전) |
| `to` | `string` | N | 조회 종료일 (`YYYY-MM-DD`, 기본값: 오늘) |

응답:

```json
{
  "statusCode": 200,
  "data": {
    "from": "2025-07-04",
    "to": "2025-07-10",
    "total_users": 1500,
    "daily_stats": [
      {
        "date": "2025-07-04",
        "attended_count": 832,
        "attendance_rate": 55.5
      },
      {
        "date": "2025-07-05",
        "attended_count": 910,
        "attendance_rate": 60.7
      },
      {
        "date": "2025-07-06",
        "attended_count": 765,
        "attendance_rate": 51.0
      },
      {
        "date": "2025-07-07",
        "attended_count": 1020,
        "attendance_rate": 68.0
      },
      {
        "date": "2025-07-08",
        "attended_count": 998,
        "attendance_rate": 66.5
      },
      {
        "date": "2025-07-09",
        "attended_count": 1043,
        "attendance_rate": 69.5
      },
      {
        "date": "2025-07-10",
        "attended_count": 874,
        "attendance_rate": 58.3
      }
    ]
  },
  "error": null
}
```

---

### 3.2 `GET /api/v1/admin/attendance/users/{user_tag}`

특정 유저의 출석 이력 및 통계를 조회합니다.

- 해당 유저의 전체 출석 통계와 날짜별 출석 이력을 반환합니다.
- 이력은 최신순으로 반환하며, 페이지네이션을 지원합니다.

요청 헤더:

- `Authorization: Bearer {access_token}`

경로 파라미터:

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| `user_tag` | `string` | Y | 조회할 유저의 `user_tag` |

쿼리 파라미터:

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| `page` | `integer` | N | 페이지 번호 (기본값: `1`) |
| `size` | `integer` | N | 페이지 크기 (기본값: `20`, 최대: `50`) |

응답:

```json
{
  "statusCode": 200,
  "data": {
    "user_tag": "pique-5010f67e",
    "summary": {
      "total_attended_days": 32,
      "current_consecutive_days": 4,
      "max_consecutive_days": 14,
      "total_points_from_attendance": 185,
      "last_attended_at": "2025-07-10T09:00:00Z"
    },
    "total_count": 32,
    "page": 1,
    "size": 20,
    "items": [
      {
        "attended_at": "2025-07-10T09:00:00Z",
        "consecutive_days": 4,
        "points_earned": 5,
        "streak_bonus_earned": false
      },
      {
        "attended_at": "2025-07-09T08:45:00Z",
        "consecutive_days": 3,
        "points_earned": 5,
        "streak_bonus_earned": false
      }
    ]
  },
  "error": null
}
```

---

## 4. 에러 코드

### 4.1 출석체크 관련 에러 코드

| HTTP | 에러 코드 | 설명 |
|------|-----------|------|
| `400` | `COMMON_INVALID_PARAMETER` | 요청 파라미터가 잘못되었습니다. |
| `401` | `AUTH_ACCESS_TOKEN_EXPIRED` | Access Token 만료 — Refresh 필요 |
| `401` | `AUTH_REFRESH_TOKEN_EXPIRED` | Refresh Token 만료 — 재로그인 필요 |
| `403` | `FORBIDDEN` | 어드민 권한이 없는 사용자 |
| `403` | `USER_BANNED` | 영구 제재된 사용자 |
| `403` | `USER_SUSPENDED` | 일정 기간 이용 정지된 사용자 |
| `404` | `USER_NOT_FOUND` | 존재하지 않는 `user_tag` |
| `409` | `ATTENDANCE_ALREADY_CHECKED` | 오늘 이미 출석체크를 완료했습니다. |
| `500` | `INTERNAL_SERVER_ERROR` | 서버 오류 |
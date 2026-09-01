# 어드민 대시보드 API 명세서

---

## 1. 설계 메모

- 모든 API는 `Authorization: Bearer {access_token}` 헤더가 필요하며, `ROLE_ADMIN` 권한을 가진 계정만 호출 가능합니다.
- DAU(활동 유저)/로그인 유저 지표는 `user_daily_activities` 테이블 기반입니다. 유저-날짜(`user_id`, `activity_date`) 조합당 row 1개이며, 두 플래그를 관리합니다.
  - `active`: 그날 인증된 API를 한 번이라도 호출했는지 (`JwtFilter`에서 매 요청마다 기록)
  - `logged_in`: 그날 로그인(소셜/로컬)했는지 (`AuthService`에서 로그인 성공 시점에만 기록, 토큰 refresh는 제외)
- `totalUserCount`는 날짜 필터와 무관하게 현재 `UserStatus.ACTIVE` 상태인 전체 유저 수(누적)입니다.

---

## 2. `GET /api/v1/admin/dashboard/summary`

오늘(서버 로컬 날짜 기준) 요약 카드용 지표를 조회합니다.

요청 헤더:

- `Authorization: Bearer {access_token}`

성공 응답 `200 OK`:

```json
{
  "statusCode": 200,
  "data": {
    "newUserCount": 12,
    "loginUserCount": 340,
    "activeUserCount": 500,
    "totalUserCount": 58000
  },
  "error": null
}
```

| 필드 | 설명 |
|---|---|
| `newUserCount` | 오늘 가입한 유저 수 (`users.created_at`) |
| `loginUserCount` | 오늘 로그인한 유저 수 (`user_daily_activities.logged_in=true`) |
| `activeUserCount` | 오늘 활동(DAU)한 유저 수 (`user_daily_activities.active=true`) |
| `totalUserCount` | 현재 `ACTIVE` 상태 전체 유저 수 (날짜 무관 누적) |

---

## 3. `GET /api/v1/admin/dashboard/dau-mau`

기간별 DAU/MAU 추이(꺾은선 그래프용)를 조회합니다. 활동이 없는 날짜도 0으로 채워서 반환합니다.

요청 헤더:

- `Authorization: Bearer {access_token}`

쿼리 파라미터:

| 파라미터 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `from` | `string` (`YYYY-MM-DD`) | Y | 조회 시작일 |
| `to` | `string` (`YYYY-MM-DD`) | Y | 조회 종료일 |
| `granularity` | `string` | N | `day`(기본값, DAU) \| `month`(MAU) |

`granularity=day`는 그날그날의 활동 유저 수(단순 카운트)를, `granularity=month`는 그날 기준 최근 30일 롤링 윈도우의 distinct 활동 유저 수(MAU)를 반환합니다. 예를 들어 `to=2026-08-15`일 때 8/15 항목의 `count`는 `2026-07-17~2026-08-15` 사이에 한 번이라도 활동한 유저 수입니다.

성공 응답 `200 OK` (`granularity=day`):

```json
{
  "statusCode": 200,
  "data": {
    "items": [
      { "date": "2026-08-01", "count": 320 },
      { "date": "2026-08-02", "count": 410 }
    ]
  },
  "error": null
}
```

예외 응답 `400 - from이 to보다 늦음`:

```json
{
  "statusCode": 400,
  "data": null,
  "error": {
    "code": "COMMON_400",
    "message": "요청 파라미터가 잘못되었습니다."
  }
}
```

---

## 4. `GET /api/v1/admin/dashboard/new-users`

기간별 신규 가입자 추이(꺾은선 그래프용)를 조회합니다. `dau-mau`와 동일한 파라미터 형태를 쓰되, 롤링 윈도우 없이 단순 카운트만 수행합니다. 가입자가 없는 날짜/주도 0으로 채워서 반환합니다.

요청 헤더:

- `Authorization: Bearer {access_token}`

쿼리 파라미터:

| 파라미터 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `from` | `string` (`YYYY-MM-DD`) | Y | 조회 시작일 |
| `to` | `string` (`YYYY-MM-DD`) | Y | 조회 종료일 |
| `granularity` | `string` | N | `day`(기본값, 일자별) \| `week`(주별, ISO 8601 월요일 시작) |

`granularity=week`일 때 각 항목의 `date`는 그 주의 시작일(월요일)입니다.

`totalCount`는 `from`~`to` 구간 전체의 정확한 합계입니다. `granularity=week`로 조회하면 주 경계가 `from`/`to`를 벗어나는 날짜까지 포함할 수 있어(예: `to`가 주 중간이면 그 주 전체가 한 항목으로 잡힘) `items`의 `count`를 그냥 더한 값과 `totalCount`가 다를 수 있습니다 — **임의 기간의 정확한 합계가 필요하면 `granularity` 값과 무관하게 `totalCount`를 사용하세요.**

성공 응답 `200 OK` (`granularity=day`):

```json
{
  "statusCode": 200,
  "data": {
    "totalCount": 20,
    "items": [
      { "date": "2026-08-01", "count": 12 },
      { "date": "2026-08-02", "count": 8 }
    ]
  },
  "error": null
}
```

성공 응답 `200 OK` (`granularity=week`):

```json
{
  "statusCode": 200,
  "data": {
    "totalCount": 130,
    "items": [
      { "date": "2026-07-27", "count": 65 },
      { "date": "2026-08-03", "count": 71 }
    ]
  },
  "error": null
}
```

`from`이 `to`보다 늦으면 `dau-mau`와 동일하게 `COMMON_400`으로 400을 반환합니다.

---

## 5. 에러 코드

| Error Code | HTTP Status | 설명 |
|---|:---:|---|
| `AUTH_403` | `403` | 해당 API 접근 권한(관리자 권한)이 없습니다. |
| `COMMON_400` | `400` | 요청 파라미터가 잘못되었습니다. (예: `from`이 `to`보다 늦은 경우) |

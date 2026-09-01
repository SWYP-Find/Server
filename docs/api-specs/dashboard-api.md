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

## 3. 에러 코드

| Error Code | HTTP Status | 설명 |
|---|:---:|---|
| `AUTH_403` | `403` | 해당 API 접근 권한(관리자 권한)이 없습니다. |

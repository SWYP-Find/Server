# 사용자 API 명세서

## 1. 설계 메모

- 사용자 API는 `snake_case` 필드명을 기준으로 합니다.
- 외부 응답에서는 내부 PK인 `user_id`를 노출하지 않고 `user_tag`를 사용합니다.
- `nickname`은 중복 허용 프로필명입니다.
- `user_tag`는 고유한 공개 식별자이며 저장 시 `@` 없이 관리합니다.
- `user_tag`는 prefix 없이 생성되는 8자리 이하의 랜덤 문자열입니다.
- 프로필 아바타는 자유 입력 이모지가 아니라 `character_type` 선택 방식으로 관리합니다.
- `character_type`은 소문자 `snake_case` 문자열 값으로 관리합니다.
- 프로필, 설정, 성향 점수는 모두 사용자 도메인 책임입니다.
- 온보딩 완료 시 필수 약관 동의 이력은 서버에서 함께 저장합니다.
- 성향 점수는 현재값을 갱신하면서 이력도 함께 적재합니다.

---

## 2. 첫 로그인 / 온보딩 API

### 2.1 `GET /api/v1/onboarding/bootstrap`

첫 로그인 화면 진입 시 필요한 초기 데이터 조회.
이모지는 8개 뿐이라 앱에서 관리하는 버전입니다.

응답:

```json
{
  "statusCode": 200,
  "data": {
    "random_nickname": "생각하는올빼미"
  },
  "error": null
}
```

### 2.2 `POST /api/v1/onboarding/profile`

첫 로그인 시 프로필 생성.
owl, wolf, lion 등은 추후 디자인에 따라 정의

요청:

```json
{
  "nickname": "생각하는올빼미",
  "character_type": "owl"
}
```

응답:

```json
{
  "statusCode": 200,
  "data": {
    "user_tag": "a7k2m9q1",
    "nickname": "생각하는올빼미",
    "character_type": "owl",
    "manner_temperature": 36.5,
    "status": "ACTIVE",
    "onboarding_completed": true
  },
  "error": null
}
```

---

## 3. 프로필 API

### 3.1 `GET /api/v1/users/{user_tag}`

공개 사용자 프로필 조회.

응답:

```json
{
  "statusCode": 200,
  "data": {
    "user_tag": "a7k2m9q1",
    "nickname": "생각하는올빼미",
    "character_type": "owl",
    "manner_temperature": 36.5
  },
  "error": null
}
```

---

### 3.2 `GET /api/v1/me/profile`

내 프로필 조회.

응답:

```json
{
  "statusCode": 200,
  "data": {
    "user_tag": "a7k2m9q1",
    "nickname": "생각하는올빼미",
    "character_type": "owl",
    "manner_temperature": 36.5,
    "updated_at": "2026-03-08T12:00:00Z"
  },
  "error": null
}
```

---

### 3.3 `PATCH /api/v1/me/profile`

닉네임 및 캐릭터 수정.

요청:

```json
{
  "nickname": "생각하는펭귄",
  "character_type": "penguin"
}
```

응답:

```json
{
  "statusCode": 200,
  "data": {
    "user_tag": "a7k2m9q1",
    "nickname": "생각하는펭귄",
    "character_type": "penguin",
    "updated_at": "2026-03-08T12:00:00Z"
  },
  "error": null
}
```

---

## 4. 설정 API

### 4.1 `GET /api/v1/me/settings`

현재 사용자 설정 조회.

응답:

```json
{
  "statusCode": 200,
  "data": {
    "push_enabled": false,
    "email_enabled": false,
    "debate_request_enabled": false,
    "profile_public": false
  },
  "error": null
}
```

---

### 4.2 `PATCH /api/v1/me/settings`

사용자 설정 수정.

요청:

```json
{
  "push_enabled": false,
  "debate_request_enabled": false
}
```

응답:

```json
{
  "statusCode": 200,
  "data": {
    "updated": true
  },
  "error": null
}
```

---

## 5. 성향 점수 API

### 5.1 `PUT /api/v1/me/tendency-scores`

최신 성향 점수 수정 및 이력 저장.
!!! 기획 확정에 따라 필드명 및 규칙 변경될 예정

요청:

```json
{
  "score_1": 30,
  "score_2": -20,
  "score_3": 55,
  "score_4": 10,
  "score_5": -75,
  "score_6": 42
}
```

응답:

```json
{
  "statusCode": 200,
  "data": {
    "user_tag": "a7k2m9q1",
    "score_1": 30,
    "score_2": -20,
    "score_3": 55,
    "score_4": 10,
    "score_5": -75,
    "score_6": 42,
    "updated_at": "2026-03-08T12:00:00Z",
    "history_saved": true
  },
  "error": null
}
```

---

### 5.2 `GET /api/v1/me/tendency-scores/history`

성향 점수 변경 이력 조회.

쿼리 파라미터:

- `cursor`: 선택
- `size`: 선택

응답:

```json
{
  "statusCode": 200,
  "data": {
    "items": [
      {
        "history_id": 1,
        "score_1": 30,
        "score_2": -20,
        "score_3": 55,
        "score_4": 10,
        "score_5": -75,
        "score_6": 42,
        "created_at": "2026-03-08T12:00:00Z"
      }
    ],
    "next_cursor": null
  },
  "error": null
}
```

---

## 6. 에러 코드

### 6.1 공통 에러 코드

| Error Code | HTTP Status | 설명 |
|------------|:-----------:|------|
| `COMMON_INVALID_PARAMETER` | `400` | 요청 파라미터 오류 |
| `AUTH_UNAUTHORIZED` | `401` | 인증 실패 |
| `AUTH_ACCESS_TOKEN_EXPIRED` | `401` | Access Token 만료 |
| `AUTH_REFRESH_TOKEN_EXPIRED` | `401` | Refresh Token 만료 — 재로그인 필요 |
| `USER_BANNED` | `403` | 영구 제재된 사용자 |
| `USER_SUSPENDED` | `403` | 일정 기간 이용 정지된 사용자 |
| `INTERNAL_SERVER_ERROR` | `500` | 서버 오류 |

### 6.2 사용자 에러 코드

| Error Code | HTTP Status | 설명 |
|------------|:-----------:|------|
| `USER_NOT_FOUND` | `404` | 존재하지 않는 사용자 |
| `ONBOARDING_ALREADY_COMPLETED` | `409` | 이미 온보딩이 완료된 사용자 |

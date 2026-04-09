# OAuth API 명세서

## 1. 설계 메모

- OAuth API는 `snake_case` 필드명을 기준으로 합니다.
- 소셜 로그인은 OAuth 2.0 인가 코드 방식을 사용합니다.
- 로그인 성공 시 서비스 자체 `access_token`, `refresh_token`을 발급합니다.
- 사용자 프로필 생성 및 온보딩 상세 명세는 `user-api.md`를 기준으로 합니다.
- 외부 응답에서는 내부 PK인 `user_id`를 노출하지 않고 `user_tag`를 사용합니다.

### 1.1 공통 요청 헤더

- `Content-Type: application/json`
    - JSON 요청 바디가 있는 API에 사용합니다.
- `Authorization: Bearer {access_token}`
    - 로그인 이후 인증이 필요한 API에 사용합니다.
- `X-Refresh-Token: {refresh_token}`
    - Access Token 재발급 API에 사용합니다.

### 1.2 토큰 사용 방식

로그인 성공 후 클라이언트는 `access_token`, `refresh_token`을 발급받습니다.

- `access_token`
    - 이후 인증이 필요한 API 호출 시 `Authorization: Bearer {access_token}` 헤더로 전달합니다.
    - 예: `GET /api/v1/me/profile`, `PATCH /api/v1/me/settings`, `DELETE /api/v1/me`
- `refresh_token`
    - API가 `401`과 `AUTH_ACCESS_TOKEN_EXPIRED`를 반환했을 때 `POST /api/v1/auth/refresh` 에서 사용합니다.
    - `X-Refresh-Token: {refresh_token}` 헤더로 전달합니다.
- Access Token 만료 안내
    - 인증이 필요한 API는 Access Token이 만료되면 `401 Unauthorized`를 반환합니다.
    - 에러 코드가 `AUTH_ACCESS_TOKEN_EXPIRED` 이면 클라이언트는 Refresh API를 호출해야 합니다.
    - Refresh 성공 후 실패했던 요청을 새 `access_token`으로 1회 재시도합니다.
- Refresh Token 만료 안내
    - Refresh API가 `401`과 `AUTH_REFRESH_TOKEN_EXPIRED`를 반환하면 재로그인이 필요합니다.
- 재발급 성공 시
    - 새 `access_token`, 새 `refresh_token`으로 교체합니다.
    - 이후 요청에는 기존 토큰 대신 새 토큰을 사용합니다.
- 로그아웃 시
    - `POST /api/v1/auth/logout` 호출 후 클라이언트에 저장된 토큰을 삭제합니다.
- 회원 탈퇴 시
    - `DELETE /api/v1/me` 호출 후 클라이언트에 저장된 토큰을 삭제합니다.

### 1.3 로그인 흐름

**신규 사용자**

1. `POST /api/v1/auth/login/{provider}` 호출
2. 응답에서 `is_new_user = true` 확인
3. 발급받은 `access_token`으로 온보딩 API 호출
4. `POST /api/v1/onboarding/profile` 완료 후 일반 사용자 API 사용

**기존 사용자**

1. `POST /api/v1/auth/login/{provider}` 호출
2. 응답에서 `is_new_user = false` 확인
3. 발급받은 `access_token`으로 바로 사용자 API 호출

---

## 2. 인증 API

### 2.1 `POST /api/v1/auth/login/{provider}`

소셜 인가 코드를 이용해 로그인 및 계정을 생성합니다.

- `{provider}`: `kakao`, `google`
- 상태가 `BANNED` 또는 `SUSPENDED`인 사용자는 `403`을 반환합니다.
- 신규 사용자는 `status = PENDING`, `is_new_user = true` 상태로 응답합니다.

요청 헤더:

- `Content-Type: application/json`

요청:

```json
{
  "authorization_code": "string",
  "redirect_uri": "string"
}
```

응답:

```json
{
  "statusCode": 200,
  "data": {
    "access_token": "eyJhbGciOiJIUzI...",
    "refresh_token": "def456-ghi789...",
    "user_tag": "a7k2m9q1",
    "is_new_user": true,
    "status": "PENDING"
  },
  "error": null
}
```

---

### 2.2 `POST /api/v1/auth/refresh`

만료된 Access Token을 Refresh Token으로 재발급합니다.

요청 헤더:

- `Content-Type: application/json`
- `X-Refresh-Token: {refresh_token}`

응답:

```json
{
  "statusCode": 200,
  "data": {
    "access_token": "new_eyJhbGciOiJIUzI...",
    "refresh_token": "new_def456-ghi789..."
  },
  "error": null
}
```

---

### 2.3 `POST /api/v1/auth/logout`

현재 로그인된 사용자의 Refresh Token을 삭제하여 로그아웃 처리합니다.

요청 헤더:

- `Content-Type: application/json`
- `Authorization: Bearer {access_token}`

응답:

```json
{
  "statusCode": 200,
  "data": {
    "logged_out": true
  },
  "error": null
}
```

---

### 2.4 `DELETE /api/v1/me`

현재 로그인된 사용자의 계정을 탈퇴 처리합니다.

- `users`, `user_social_accounts`, `auth_refresh_tokens` 연관 데이터를 함께 처리합니다.
- 사용자 도메인 상세 정리는 `user` 정책에 따라 함께 처리합니다.
- 탈퇴 사유는 별도 이력 테이블에 저장합니다.

요청 헤더:

- `Authorization: Bearer {access_token}`

요청 바디:

```json
{
  "reason": "NO_TIME"
}
```

가능한 `reason` 값:

- `NOT_USED_OFTEN`
- `NO_INTERESTING_BATTLES`
- `BATTLE_STYLE_NOT_FIT`
- `SERVICE_INCONVENIENT`
- `NO_TIME`
- `OTHER`

응답:

```json
{
  "statusCode": 200,
  "data": {
    "withdrawn": true
  },
  "error": null
}
```

---

## 3. 에러 코드

### 3.1 공통 에러 코드

| HTTP | 에러 코드 | 설명 |
|------|-----------|------|
| `400` | `COMMON_INVALID_PARAMETER` | 요청 파라미터가 잘못되었습니다. |
| `401` | `AUTH_INVALID_CODE` | 유효하지 않은 소셜 인가 코드 |
| `401` | `AUTH_ACCESS_TOKEN_EXPIRED` | Access Token 만료 — Refresh 필요 |
| `401` | `AUTH_REFRESH_TOKEN_EXPIRED` | Refresh Token 만료 — 재로그인 필요 |
| `403` | `USER_BANNED` | 영구 제재된 사용자 |
| `403` | `USER_SUSPENDED` | 일정 기간 이용 정지된 사용자 |
| `500` | `INTERNAL_SERVER_ERROR` | 서버 오류 |

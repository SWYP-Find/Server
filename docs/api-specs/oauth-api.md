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
  - 보호 API가 `401`과 `auth_access_token_expired`를 반환했을 때 `POST /api/v1/auth/refresh` 에서 사용합니다.
  - `X-Refresh-Token: {refresh_token}` 헤더로 전달합니다.
- Access Token 만료 안내
  - 인증이 필요한 API는 Access Token이 만료되면 `401 Unauthorized`를 반환합니다.
  - 에러 코드가 `auth_access_token_expired` 이면 클라이언트는 Refresh API를 호출해야 합니다.
  - Refresh 성공 후 실패했던 요청을 새 `access_token`으로 1회 재시도합니다.
- Refresh Token 만료 안내
  - Refresh API가 `401`과 `auth_refresh_token_expired`를 반환하면 재로그인이 필요합니다.
- 재발급 성공 시
  - 새 `access_token`, 새 `refresh_token`으로 교체합니다.
  - 이후 요청에는 기존 토큰 대신 새 토큰을 사용합니다.
- 로그아웃 시
  - `POST /api/v1/auth/logout` 호출 후 클라이언트에 저장된 토큰을 삭제합니다.
- 회원 탈퇴 시
  - `DELETE /api/v1/me` 호출 후 클라이언트에 저장된 토큰을 삭제합니다.

신규 사용자 흐름:

1. `POST /api/v1/auth/login/{provider}` 호출
2. 응답에서 `is_new_user = true` 확인
3. 발급받은 `access_token`으로 온보딩 API 호출
4. `POST /api/v1/onboarding/profile` 완료 후 일반 사용자 API 사용

기존 사용자 흐름:

1. `POST /api/v1/auth/login/{provider}` 호출
2. 응답에서 `is_new_user = false` 확인
3. 발급받은 `access_token`으로 바로 사용자 API 호출

---

## 2. 인증 API

### 2.1 `POST /api/v1/auth/login/{provider}`

소셜 인가 코드를 이용해 로그인 및 계정을 생성합니다.

- `{provider}`: `kakao`, `google`, `apple`
- 상태가 `BANNED`인 사용자는 `403`을 반환합니다.
- 신규 사용자는 `status = PENDING`, `is_new_user = true` 상태로 응답합니다.

요청:

```json
{
  "authorization_code": "string",
  "redirect_uri": "string"
}
```

요청 헤더:

- `Content-Type: application/json`

응답:

```json
{
  "access_token": "eyJhbGciOiJIUzI...",
  "refresh_token": "def456-ghi789...",
  "user_tag": "sfit4-2",
  "is_new_user": true,
  "status": "PENDING"
}
```

### 2.2 `POST /api/v1/auth/refresh`

만료된 Access Token을 Refresh Token으로 재발급합니다.

요청 헤더:

- `Content-Type: application/json`
- `X-Refresh-Token: {refresh_token}`

응답:

```json
{
  "access_token": "new_eyJhbGciOiJIUzI...",
  "refresh_token": "new_def456-ghi789..."
}
```

### 2.3 `POST /api/v1/auth/logout`

현재 로그인된 사용자의 Refresh Token을 삭제하여 로그아웃 처리합니다.

요청 헤더:

- `Content-Type: application/json`
- `Authorization: Bearer {access_token}`

응답:

```json
{
  "logged_out": true
}
```

### 2.4 `DELETE /api/v1/me`

현재 로그인된 사용자의 계정을 탈퇴 처리합니다.

- `users`, `user_social_accounts`, `auth_refresh_tokens` 연관 데이터를 함께 처리합니다.
- 사용자 도메인 상세 정리는 `user` 정책에 따라 함께 처리합니다.

요청 헤더:

- `Authorization: Bearer {access_token}`

응답:

```json
{
  "withdrawn": true
}
```

---

## 3. 인증 예외 응답

### 3.1 잘못된 요청 (400)

```json
{
  "code": "common_invalid_parameter",
  "message": "요청 파라미터가 잘못되었습니다.",
  "errors": [
    {
      "field": "redirect_uri",
      "value": "",
      "reason": "redirect_uri 는 필수입니다."
    }
  ]
}
```

### 3.2 인증 실패 (401)

```json
{
  "code": "auth_invalid_code",
  "message": "유효하지 않은 소셜 인가 코드입니다.",
  "errors": []
}
```

```json
{
  "code": "auth_access_token_expired",
  "message": "Access Token이 만료되었습니다. Refresh Token으로 재발급이 필요합니다.",
  "errors": []
}
```

```json
{
  "code": "auth_refresh_token_expired",
  "message": "Refresh Token이 만료되었거나 유효하지 않습니다. 다시 로그인이 필요합니다.",
  "errors": []
}
```

### 3.3 접근 거부 (403)

```json
{
  "code": "user_banned",
  "message": "제재된 사용자입니다.",
  "errors": []
}
```

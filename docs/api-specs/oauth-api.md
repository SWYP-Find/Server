# 🔐 PIQUE 사용자 인증 및 온보딩 API 통합 명세서

## 1. 설계 메모
* **인증 방식**: OAuth 2.0 인가 코드 방식을 사용하며, 서비스 자체 JWT(Access/Refresh)를 발급합니다.
* **온보딩 흐름**: 로그인 응답의 `isNewUser`가 `true`일 경우, `bootstrap` 데이터를 조회한 뒤 `profile` 생성 API를 호출합니다.
* **상태 전환**: 프로필 생성이 완료되면 유저 상태(`status`)는 `PENDING`에서 `ACTIVE`로 변경됩니다.
* **재화 관리**: 유저 지갑(`userWallet`)은 별도 테이블로 관리하며 프로필 설정 완료 시 함께 조회됩니다.
* **응답 규격**: 모든 응답은 `statusCode`, `data`, `error` 필드를 포함하는 공통 포맷을 준수합니다.

---

## 2. API 상세 내역

### 2.1 소셜 로그인 및 회원가입
* **Endpoint**: `POST /api/v1/auth/login/{provider}`
* **설명**: 소셜 인가 코드를 이용해 로그인 및 계정을 생성합니다.
* **요청 바디**:
```json
{
  "authorizationCode": "string",
  "redirectUri": "string"
}
```
* **성공 응답**:
```json
{
  "statusCode": 200,
  "data": {
    "accessToken": "eyJhbGciOiJIUzI...",
    "refreshToken": "def456-ghi789...",
    "userId": 105,
    "isNewUser": true,
    "status": "PENDING"
  },
  "error": null
}
```

### 2.2 온보딩 초기 데이터 조회
* **Endpoint**: `GET /api/v1/onboarding/bootstrap`
* **설명**: 첫 로그인 화면 진입 시 필요한 랜덤 닉네임과 캐릭터 옵션을 조회합니다.
* **성공 응답**:
```json
{
  "statusCode": 200,
  "data": {
    "randomNickname": "생각하는올빼미",
    "characterOptions": [
      { "id": 1, "name": "올빼미", "imageUrl": "https://..." },
      { "id": 2, "name": "여우", "imageUrl": "https://..." }
    ]
  },
  "error": null
}
```

### 2.3 초기 프로필 설정 (가입 완료)
* **Endpoint**: `POST /api/v1/onboarding/profile`
* **설명**: 신규 유저의 닉네임과 캐릭터를 설정하여 정식 회원으로 전환합니다.
* **요청 바디**:
```json
{
  "nickname": "생각하는올빼미",
  "characterId": 1
}
```
* **성공 응답**:
```json
{
  "statusCode": 200,
  "data": {
    "userId": 105,
    "nickname": "생각하는올빼미",
    "characterId": 1,
    "userWallet": {
      "credit": 500,
      "updatedAt": "2026-03-08T12:00:00Z"
    },
    "status": "ACTIVE",
    "onboardingCompleted": true
  },
  "error": null
}
```

### 2.4 토큰 재발급
* **Endpoint**: `POST /api/v1/auth/refresh`
* **설명**: 만료된 Access Token을 Refresh Token을 사용하여 재발급합니다.
* **요청 헤더**: `X-Refresh-Token: {refreshToken}`
* **성공 응답**:
```json
{
  "statusCode": 200,
  "data": {
    "accessToken": "new_eyJhbGciOiJIUzI...",
    "refreshToken": "new_def456-ghi789..."
  },
  "error": null
}
```

---

## 3. 예외 응답 (공통)

### 3.1 요청 파라미터 오류 (400)
```json
{
  "statusCode": 400,
  "data": null,
  "error": {
    "code": "COMMON_INVALID_PARAMETER",
    "message": "요청 파라미터가 잘못되었습니다.",
    "errors": [
      {
        "field": "nickname",
        "value": "홍길동!",
        "reason": "특수문자는 포함할 수 없습니다."
      }
    ]
  }
}
```

### 3.2 인증 오류 (401)
```json
{
  "statusCode": 401,
  "data": null,
  "error": {
    "code": "AUTH_INVALID_CODE",
    "message": "유효하지 않은 소셜 인가 코드입니다.",
    "errors": []
  }
}
```
```json
{
  "statusCode": 401,
  "data": null,
  "error": {
    "code": "AUTH_TOKEN_EXPIRED",
    "message": "만료되었거나 유효하지 않은 Refresh Token입니다.",
    "errors": []
  }
}
```

### 3.3 중복 오류 (409)
```json
{
  "statusCode": 409,
  "data": null,
  "error": {
    "code": "USER_NICKNAME_DUPLICATE",
    "message": "이미 사용 중인 닉네임입니다.",
    "errors": []
  }
}
```
```json
{
  "statusCode": 409,
  "data": null,
  "error": {
    "code": "ONBOARDING_ALREADY_COMPLETED",
    "message": "이미 온보딩이 완료된 사용자입니다.",
    "errors": []
  }
}
```
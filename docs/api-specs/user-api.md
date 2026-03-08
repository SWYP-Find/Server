# 사용자 API 명세서

## 1. 설계 메모

- 첫 로그인 시 닉네임 랜덤 생성과 이모지 선택이 필요합니다.
- 프로필, 설정, 성향 점수는 모두 사용자 도메인 책임입니다.
- 성향 점수는 현재값을 갱신하면서 이력도 함께 적재합니다.

---

## 2. 첫 로그인 API

### 2.1 `GET /api/v1/onboarding/bootstrap`

첫 로그인 화면 진입 시 필요한 초기 데이터 조회.

응답:

```json
{
  "random_nickname": "생각하는올빼미",
  "emoji_options": ["🦊", "🦉", "🐱", "🐻", "🐰", "🦁", "🐸", "🐧"]
}
```

### 2.2 `POST /api/v1/onboarding/profile`

첫 로그인 시 프로필 생성.

요청:

```json
{
  "nickname": "생각하는올빼미",
  "avatar_emoji": "🦉"
}
```

응답:

```json
{
  "user_id": "user_001",
  "nickname": "생각하는올빼미",
  "avatar_emoji": "🦉",
  "manner_temperature": 36.5,
  "onboarding_completed": true
}
```

---

## 3. 프로필 API

### 3.1 `PATCH /api/v1/me/profile`

닉네임 및 아바타 수정.

요청:

```json
{
  "nickname": "생각하는펭귄",
  "avatar_emoji": "🐧"
}
```

응답:

```json
{
  "user_id": "user_001",
  "nickname": "생각하는펭귄",
  "avatar_emoji": "🐧",
  "updated_at": "2026-03-08T12:00:00Z"
}
```

---

## 4. 설정 API

### 4.1 `GET /api/v1/me/settings`

현재 사용자 설정 조회.

응답:

```json
{
  "push_enabled": true,
  "email_enabled": false,
  "debate_request_enabled": true,
  "profile_public": true
}
```

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
  "updated": true
}
```

---

## 5. 성향 점수 API

### 5.1 `PUT /api/v1/me/tendency-scores`

최신 성향 점수 수정 및 이력 저장.

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
  "user_id": "user_001",
  "score_1": 30,
  "score_2": -20,
  "score_3": 55,
  "score_4": 10,
  "score_5": -75,
  "score_6": 42,
  "updated_at": "2026-03-08T12:00:00Z",
  "history_saved": true
}
```

### 5.2 `GET /api/v1/me/tendency-scores/history`

성향 점수 변경 이력 조회.

쿼리 파라미터:

- `cursor`: 선택
- `size`: 선택

응답:

```json
{
  "items": [
    {
      "history_id": "ths_001",
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
}
```
